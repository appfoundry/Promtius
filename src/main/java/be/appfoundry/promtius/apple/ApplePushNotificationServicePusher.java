package be.appfoundry.promtius.apple;

import be.appfoundry.promtius.ClientToken;
import be.appfoundry.promtius.ClientTokenFactory;
import be.appfoundry.promtius.ClientTokenService;
import be.appfoundry.promtius.ClientTokenType;
import be.appfoundry.promtius.PushPayload;
import be.appfoundry.promtius.Pusher;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author Mike Seghers
 */
public class ApplePushNotificationServicePusher implements Pusher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplePushNotificationServicePusher.class);

    private final ApnsService apnsService;
    private final ClientTokenService<String> clientTokenService;
    private final ClientTokenFactory<String> clientTokenFactory;
    private final ClientTokenType clientTokenType;

    public ApplePushNotificationServicePusher(ApnsService apnsService, ClientTokenService<String> clientTokenService, ClientTokenFactory<String> clientTokenFactory, final ClientTokenType clientTokenType) {
        this.apnsService = apnsService;
        this.clientTokenService = clientTokenService;
        this.clientTokenFactory = clientTokenFactory;
        this.clientTokenType = clientTokenType;
    }

    @Override
    public void sendPush(final PushPayload payload) {
        LOGGER.info("sending payload ({}) to APNS", payload);
        Map<String,Date> inactiveDevices = apnsService.getInactiveDevices();
        LOGGER.debug("Unregistering device tokens ({})", inactiveDevices);
        for(String token : inactiveDevices.keySet()) {
            clientTokenService.unregisterClientToken(clientTokenFactory.createClientToken(token, clientTokenType));
        }

        List<ClientToken<String>> tokens = clientTokenService.findClientTokensForOperatingSystem(clientTokenType);
        List<String> tokenIds = new ArrayList<String>(tokens.size());
        LOGGER.info("Pushing with tokens ({})", tokenIds);
        for (ClientToken<String> token : tokens) {
            tokenIds.add(token.getToken());
        }

        String payloadAsString = APNS.newPayload().alertBody(payload.getMessage()).build();
        apnsService.push(tokenIds, payloadAsString);
        LOGGER.info("APNS push finished", payload);
    }
}
