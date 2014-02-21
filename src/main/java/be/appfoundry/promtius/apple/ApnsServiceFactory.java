package be.appfoundry.promtius.apple;

import com.google.common.base.Strings;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsNotification;
import com.notnoop.apns.ApnsService;
import com.notnoop.apns.ApnsServiceBuilder;
import com.notnoop.apns.EnhancedApnsNotification;
import com.notnoop.exceptions.NetworkIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * @author Mike Seghers
 */
public class ApnsServiceFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApnsServiceFactory.class);

    public static ApnsService getSanboxApnsService(String certPath, String certPass) {
        return internalGetApnsService(certPath, certPass, true);
    }

    public static ApnsService getProductionApnsService(String certPath, String certPass) {
        return internalGetApnsService(certPath, certPass, false);
    }

    private static ApnsService internalGetApnsService(String certPath, String certPass, boolean sandbox) {
        ApnsService service;
        if (Strings.isNullOrEmpty(certPass)) {
            service = fallBackToNoopService();
        } else {
            LOGGER.info("APNS configuration seems ok, setting up APNS.");
            URL resource = ApplePushNotificationServicePusher.class.getResource(certPath);
            ApnsServiceBuilder apnsServiceBuilder = APNS.newService().withCert(resource.getPath(), certPass);
            if (sandbox) {
                LOGGER.warn("APNS setup for sandbox");
                apnsServiceBuilder.withSandboxDestination();
            } else {
                LOGGER.warn("APNS setup for production");
                apnsServiceBuilder.withProductionDestination();
            }
            service = apnsServiceBuilder.build();
        }
        return service;
    }

    private static ApnsService fallBackToNoopService() {
        LOGGER.warn("APNS Service not configured properly, falling back to NO-OP service, your push notifications will not be send to Apple!");
        return new ApnsService() {
            @Override
            public ApnsNotification push(String deviceToken, String payload) throws NetworkIOException {
                LOGGER.warn("Push will not be send, APNS not configured properly");
                return null;
            }

            @Override
            public EnhancedApnsNotification push(String deviceToken, String payload, Date expiry) throws NetworkIOException {
                LOGGER.warn("Push will not be send, APNS not configured properly");
                return null;
            }

            @Override
            public ApnsNotification push(byte[] deviceToken, byte[] payload) throws NetworkIOException {
                LOGGER.warn("Push will not be send, APNS not configured properly");
                return null;
            }

            @Override
            public EnhancedApnsNotification push(byte[] deviceToken, byte[] payload, int expiry) throws NetworkIOException {
                LOGGER.warn("Push will not be send, APNS not configured properly");
                return null;
            }

            @Override
            public Collection<? extends ApnsNotification> push(Collection<String> deviceTokens, String payload) throws NetworkIOException {
                LOGGER.warn("Push will not be send, APNS not configured properly");
                return Collections.emptyList();
            }

            @Override
            public Collection<? extends EnhancedApnsNotification> push(Collection<String> deviceTokens, String payload, Date expiry) throws NetworkIOException {
                LOGGER.warn("Push will not be send, APNS not configured properly");
                return Collections.emptyList();
            }

            @Override
            public Collection<? extends ApnsNotification> push(Collection<byte[]> deviceTokens, byte[] payload) throws NetworkIOException {
                LOGGER.warn("Push will not be send, APNS not configured properly");
                return Collections.emptyList();
            }

            @Override
            public Collection<? extends EnhancedApnsNotification> push(Collection<byte[]> deviceTokens, byte[] payload, int expiry) throws NetworkIOException {
                LOGGER.warn("Push will not be send, APNS not configured properly");
                return Collections.emptyList();
            }

            @Override
            public void push(ApnsNotification message) throws NetworkIOException {
                LOGGER.warn("Push will not be send, APNS not configured properly");
            }

            @Override
            public void start() {
            }

            @Override
            public void stop() {
            }

            @Override
            public Map<String, Date> getInactiveDevices() throws NetworkIOException {
                LOGGER.warn("Inactive devices not recoverable, APNS not configured properly");
                return Collections.emptyMap();
            }

            @Override
            public void testConnection() throws NetworkIOException {
                LOGGER.warn("APNS connection not tested, APNS not configured properly");
            }
        };
    }
}
