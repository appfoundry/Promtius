package be.appfoundry.promtius.apple;

import be.appfoundry.promtius.ClientToken;
import be.appfoundry.promtius.ClientTokenFactory;
import be.appfoundry.promtius.ClientTokenService;
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
public class ApplePushNotificationServicePusher<P> implements Pusher<P> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplePushNotificationServicePusher.class);

    private final ApnsService apnsService;
    private final ClientTokenService<String, P> clientTokenService;
    private final ClientTokenFactory<String, P> clientTokenFactory;
    private final P platform;

    public ApplePushNotificationServicePusher(ApnsService apnsService, ClientTokenService<String, P> clientTokenService, ClientTokenFactory<String, P> clientTokenFactory,
                                              final P platform) {
        this.apnsService = apnsService;
        this.clientTokenService = clientTokenService;
        this.clientTokenFactory = clientTokenFactory;
        this.platform = platform;
    }

    @Override
    public void sendPush(final PushPayload payload) {
        LOGGER.info("Sending payload ({}) to APNs", payload);
        Map<String, Date> inactiveDevices = apnsService.getInactiveDevices();
        LOGGER.debug("Unregistering device tokens ({})", inactiveDevices);
        for (String token : inactiveDevices.keySet()) {
            clientTokenService.unregisterClientToken(clientTokenFactory.createClientToken(token, platform));
        }

        List<ClientToken<String, P>> tokens = clientTokenService.findClientTokensForOperatingSystem(platform);
        List<String> tokenIds = new ArrayList<String>(tokens.size());
        LOGGER.debug("Pushing with tokens ({})", tokenIds);
        for (ClientToken<String, P> token : tokens) {
            tokenIds.add(token.getToken());
        }

        String payloadAsString = APNS.newPayload().alertBody(payload.getMessage()).build();
        apnsService.push(tokenIds, payloadAsString);
        LOGGER.info("APNs push finished", payload);
    }

    @Override
    public P getPlatform() {
        return platform;
    }
}
