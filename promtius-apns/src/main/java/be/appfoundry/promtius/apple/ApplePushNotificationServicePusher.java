package be.appfoundry.promtius.apple;

import be.appfoundry.promtius.ClientToken;
import be.appfoundry.promtius.ClientTokenFactory;
import be.appfoundry.promtius.ClientTokenService;
import be.appfoundry.promtius.PushPayload;
import be.appfoundry.promtius.Pusher;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * A {@link be.appfoundry.promtius.Pusher} capable of sending payload via Apple's Push Notification Services.
 *
 * @param <CT> The type of ClientTokens this pusher is using
 * @param <P>  The platform identifier type, identifying the platform to which the pusher pushes its messages.
 * @param <G>  The type of the group identifier. A group identifier is used to put client tokens in a collection of groups, so that a push can be done to specific groups.
 * @author Mike Seghers
 */
public class ApplePushNotificationServicePusher<CT extends ClientToken<String, P>, P, G> implements Pusher<P, G> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplePushNotificationServicePusher.class);

    private final ApnsService apnsService;
    private final ClientTokenService<CT, String, P, G> clientTokenService;
    private final ClientTokenFactory<CT, String, P> clientTokenFactory;
    private final P platform;

    public ApplePushNotificationServicePusher(ApnsService apnsService, ClientTokenService<CT, String, P, G> clientTokenService,
                                              ClientTokenFactory<CT, String, P> clientTokenFactory, final P platform) {
        this.apnsService = apnsService;
        this.clientTokenService = clientTokenService;
        this.clientTokenFactory = clientTokenFactory;
        this.platform = platform;
    }

    @Override
    public void sendPush(final PushPayload payload) {
        LOGGER.info("Sending payload ({}) to APNs", payload);
        unregisterInactiveDevices();
        pushPayloadToClientsIdentifiedByTokens(payload, clientTokenService.findClientTokensForOperatingSystem(platform));
        LOGGER.info("APNs push finished", payload);
    }

    @Override
    public void sendPush(final PushPayload payload, final Collection<G> groups) {
        LOGGER.info("Sending payload ({}) to APNs", payload);
        unregisterInactiveDevices();
        pushPayloadToClientsIdentifiedByTokens(payload, clientTokenService.findClientTokensForOperatingSystem(platform, groups));
        LOGGER.info("APNs push finished", payload);
    }

    private void pushPayloadToClientsIdentifiedByTokens(final PushPayload payload, final List<CT> tokens) {
        List<String> tokenIds = Lists.transform(tokens, new Function<ClientToken<String, P>, String>() {
            @Override
            public String apply(final ClientToken<String, P> input) {
                return input.getToken();
            }
        });

        LOGGER.debug("Pushing payload to {} devices", tokenIds.size());
        String payloadAsString = APNS.newPayload().alertBody(payload.getMessage()).sound(payload.getSound()).build();
        apnsService.push(tokenIds, payloadAsString);
    }

    private void unregisterInactiveDevices() {
        Map<String, Date> inactiveDevices = apnsService.getInactiveDevices();
        LOGGER.debug("Unregistering device tokens ({})", inactiveDevices);
        for (String token : inactiveDevices.keySet()) {
            clientTokenService.unregisterClientToken(clientTokenFactory.createClientToken(token, platform));
        }
    }

    @Override
    public P getPlatform() {
        return platform;
    }
}
