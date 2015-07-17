package be.appfoundry.promtius.google;

import be.appfoundry.custom.google.android.gcm.server.Constants;
import be.appfoundry.custom.google.android.gcm.server.Message;
import be.appfoundry.custom.google.android.gcm.server.MulticastResult;
import be.appfoundry.custom.google.android.gcm.server.Result;
import be.appfoundry.promtius.ClientToken;
import be.appfoundry.promtius.ClientTokenFactory;
import be.appfoundry.promtius.ClientTokenService;
import be.appfoundry.promtius.PushPayload;
import be.appfoundry.promtius.Pusher;
import be.appfoundry.promtius.exception.PushFailedException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @param <CT> The type of ClientTokens this pusher is using
 * @param <P> The platform identifier type, identifying the platform to which the pusher pushes its messages.
 * @param <G> The type of the group identifier. A group identifier is used to put client tokens in a collection of groups, so that a push can be done to specific groups.
 * @author Mike Seghers
 */
public final class GoogleCloudMessagingPusher<CT extends ClientToken<String, P>, P, G> implements Pusher<P, G> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleCloudMessagingPusher.class);

    public static final int MAX_MULTICAST_SIZE = 1000;
    private final GoogleSenderWrapper senderWrapper;
    private final ClientTokenService<CT, String, P, G> clientTokenService;
    private final P platform;
    private final ClientTokenFactory<CT, String, P> clientTokenFactory;

    public GoogleCloudMessagingPusher(final GoogleSenderWrapper senderWrapper, final ClientTokenService<CT, String, P, G> infoService,
                                      final ClientTokenFactory<CT, String, P> clientTokenFactory, final P platform) {
        this.senderWrapper = senderWrapper;
        this.clientTokenService = infoService;
        this.clientTokenFactory = clientTokenFactory;
        this.platform = platform;
    }

    @Override
    public void sendPush(final PushPayload payload) {
        LOGGER.info("Sending payload ({}) to GCM", payload);
        List<CT> tokens = clientTokenService.findClientTokensForOperatingSystem(platform);
        pushPayloadToClientsIdentifiedByTokens(payload, tokens);
        LOGGER.info("GCM push finished", payload);
    }

    @Override
    public void sendPush(final PushPayload payload, final Collection<G> groups) {
        LOGGER.info("Sending payload ({}) to groups {}", payload, groups);
        List<CT> tokens = clientTokenService.findClientTokensForOperatingSystem(platform, groups);
        pushPayloadToClientsIdentifiedByTokens(payload, tokens);
        LOGGER.info("GCM group push finished", payload);
    }

    private void pushPayloadToClientsIdentifiedByTokens(final PushPayload payload, final List<CT> tokens) {
        Message.Builder builder = new Message.Builder().addData("message", payload.getMessage()).addData("sound", payload.getSound()).collapseKey(payload.getDiscriminator());
        if (payload.getCustomFields().isPresent()) {
            Map<String, ?> customFields = payload.getCustomFields().get();
            builder.addData("data", customFields);
        }
        if (payload.getTimeToLive().isPresent()) {
            Integer ttl = payload.getTimeToLive().get();
            builder.timeToLive(ttl * 60);

        }
        Message message = builder.build();


        List<String> partialDeviceIds = new ArrayList<>();
        int counter = 0;
        for (ClientToken<String, P> token : tokens) {
            partialDeviceIds.add(token.getToken());
            counter++;
            if (counter == MAX_MULTICAST_SIZE) {
                counter = 0;
                sendMessageBatch(ImmutableList.copyOf(partialDeviceIds), message);
                partialDeviceIds.clear();
            }
        }

        if (!partialDeviceIds.isEmpty()) {
            sendMessageBatch(partialDeviceIds, message);
        }
    }

    private void sendMessageBatch(final List<String> partialDeviceIds, final Message message) {
        try {
            MulticastResult result = senderWrapper.send(message, partialDeviceIds, 5);
            if (resultNeedsProcessing(result)) {
                processMulticastResult(partialDeviceIds, result);
            }

        } catch (IOException e) {
            throw new PushFailedException("sender threw exception for message " + message, e);
        }
    }

    private boolean resultNeedsProcessing(final MulticastResult result) {
        return result != null && (result.getCanonicalIds() > 0 || result.getFailure() > 0);
    }

    private void processMulticastResult(final List<String> partialDeviceIds, final MulticastResult multicastResult) {
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

    private void checkError(final String regId, final Result result) {
        String err = result.getErrorCodeName();
        if (Constants.ERROR_NOT_REGISTERED.equals(err)) {
            clientTokenService.unregisterClientToken(clientTokenFactory.createClientToken(regId, platform));
        }
    }

    private void checkShouldReplaceDeviceId(final String existingId, final Result result) {
        String canId = result.getCanonicalRegistrationId();
        if (canId != null) {
            replaceDeviceId(existingId, canId);
        }
    }

    private void replaceDeviceId(final String oldId, final String newId) {
        clientTokenService.unregisterClientToken(clientTokenFactory.createClientToken(oldId, platform));
        clientTokenService.registerClientToken(clientTokenFactory.createClientToken(newId, platform));
    }


    @Override
    public Set<P> getPlatforms() {
        return ImmutableSet.of(platform);
    }
}
