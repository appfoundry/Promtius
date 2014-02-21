package be.appfoundry.promtius.google;

import be.appfoundry.promtius.ClientToken;
import be.appfoundry.promtius.ClientTokenFactory;
import be.appfoundry.promtius.ClientTokenService;
import be.appfoundry.promtius.ClientTokenType;
import be.appfoundry.promtius.exception.PushFailedException;
import be.appfoundry.promtius.PushPayload;
import be.appfoundry.promtius.Pusher;
import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Result;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mike Seghers
 */
public class GoogleCloudMessagingPusher implements Pusher {

    public static final String COLLAPSE_KEY = "kolepski";
    public static final int MAX_MULTICAST_SIZE = 1000;
    private GoogleSenderWrapper senderWrapper;
    private ClientTokenService<String> clientTokenService;
    private final ClientTokenType clientTokenType;
    private ClientTokenFactory<String> clientTokenFactory;

    public GoogleCloudMessagingPusher(final GoogleSenderWrapper senderWrapper, final ClientTokenService infoService, final ClientTokenType clientTokenType) {
        this.senderWrapper = senderWrapper;
        this.clientTokenService = infoService;
        this.clientTokenType = clientTokenType;
    }

    @Override
    public void sendPush(PushPayload payload) {
        //payload to message, and send via sender
        List<ClientToken<String>> tokens = clientTokenService.findClientTokensForOperatingSystem(clientTokenType);

        List<String> partialDeviceIds = new ArrayList<String>();
        int counter = 0;
        Message message = new Message.Builder().addData("message", payload.getMessage()).collapseKey(COLLAPSE_KEY).build();

        for (ClientToken<String> token : tokens) {
            partialDeviceIds.add(token.getToken());
            counter++;
            if (counter == MAX_MULTICAST_SIZE) {
                counter = 0;
                sendMessageBatch(partialDeviceIds, message);
                partialDeviceIds = new ArrayList<String>();
            }
        }

        if (!partialDeviceIds.isEmpty()) {
            sendMessageBatch(partialDeviceIds, message);
        }
    }

    private void sendMessageBatch(List<String> partialDeviceIds, Message message) {
        try {
            MulticastResult result = senderWrapper.send(message, partialDeviceIds, 5);
            if (resultNeedsProcessing(result)) {
                processMulticastResult(partialDeviceIds, result);
            }

        } catch (IOException e) {
            throw new PushFailedException("sender threw exception for message " + message, e);
        }
    }

    private boolean resultNeedsProcessing(MulticastResult result) {
        return result != null && (result.getCanonicalIds() > 0 || result.getFailure() > 0);
    }

    private void processMulticastResult(List<String> partialDeviceIds, MulticastResult multicastResult) {
        List<Result> results = multicastResult.getResults();
        for (int i = 0; i < partialDeviceIds.size(); i++) {
            String regId = partialDeviceIds.get(i);
            Result result = results.get(i);
            if (result.getMessageId() != null) {
                checkShouldReplaceDeviceId(regId, result);
            } else {
                checkError(regId, result);
            }
        }
    }

    private void checkError(String regId, Result result) {
        String err = result.getErrorCodeName();
        if (Constants.ERROR_NOT_REGISTERED.equals(err)) {
            clientTokenService.unregisterClientToken(clientTokenFactory.createClientToken(regId, clientTokenType));
        }
    }

    private void checkShouldReplaceDeviceId(String existingId, Result result) {
        String canId = result.getCanonicalRegistrationId();
        if (canId != null) {
            replaceDeviceId(existingId, canId);
        }
    }

    private void replaceDeviceId(String oldId, String newId) {
        clientTokenService.unregisterClientToken(clientTokenFactory.createClientToken(oldId, clientTokenType));
        clientTokenService.registerClientToken(clientTokenFactory.createClientToken(newId, clientTokenType));
    }


}
