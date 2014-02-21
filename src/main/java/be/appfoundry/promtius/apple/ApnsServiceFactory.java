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

    public static ApnsService getSanboxApnsService(final String certPath, final String certPass) {
        return internalGetApnsService(certPath, certPass, true);
    }

    public static ApnsService getProductionApnsService(final String certPath, final String certPass) {
        return internalGetApnsService(certPath, certPass, false);
    }

    private static ApnsService internalGetApnsService(final String certPath, final String certPass, final boolean sandbox) {
        ApnsService service;
        if (Strings.isNullOrEmpty(certPass)) {
            service = fallBackToNoOpService();
        } else {
            LOGGER.info("APNs configuration seems ok, setting up APNs.");
            ApnsServiceBuilder apnsServiceBuilder = getApnsServiceBuilderWithCertInfo(certPath, certPass);
            if (sandbox) {
                LOGGER.info("APNs setup for sandbox");
                apnsServiceBuilder.withSandboxDestination();
            } else {
                LOGGER.info("APNs setup for production");
                apnsServiceBuilder.withProductionDestination();
            }
            service = apnsServiceBuilder.build();
        }
        return service;
    }

    private static ApnsServiceBuilder getApnsServiceBuilderWithCertInfo(final String certPath, final String certPass) {
        URL resource = ApplePushNotificationServicePusher.class.getResource(certPath);
        return APNS.newService().withCert(resource.getPath(), certPass);
    }

    private static ApnsService fallBackToNoOpService() {
        LOGGER.warn("APNs Service not configured properly, falling back to NO-OP service, your push notifications will not be send to Apple!");
        return new ApnsService() {
            @Override
            public ApnsNotification push(String deviceToken, String payload) throws NetworkIOException {
                return logWarningAndReturn(null);
            }

            @Override
            public EnhancedApnsNotification push(String deviceToken, String payload, Date expiry) throws NetworkIOException {
                return logWarningAndReturn(null);
            }

            @Override
            public ApnsNotification push(byte[] deviceToken, byte[] payload) throws NetworkIOException {
                return logWarningAndReturn(null);
            }

            @Override
            public EnhancedApnsNotification push(byte[] deviceToken, byte[] payload, int expiry) throws NetworkIOException {
                return logWarningAndReturn(null);
            }

            @Override
            public Collection<? extends ApnsNotification> push(Collection<String> deviceTokens, String payload) throws NetworkIOException {
                return logWarningAndReturn(Collections.<ApnsNotification>emptyList());
            }

            @Override
            public Collection<? extends EnhancedApnsNotification> push(Collection<String> deviceTokens, String payload, Date expiry) throws NetworkIOException {
                return logWarningAndReturn(Collections.<EnhancedApnsNotification>emptyList());
            }

            @Override
            public Collection<? extends ApnsNotification> push(Collection<byte[]> deviceTokens, byte[] payload) throws NetworkIOException {
                return logWarningAndReturn(Collections.<ApnsNotification>emptyList());
            }

            @Override
            public Collection<? extends EnhancedApnsNotification> push(Collection<byte[]> deviceTokens, byte[] payload, int expiry) throws NetworkIOException {
                return logWarningAndReturn(Collections.<EnhancedApnsNotification>emptyList());
            }

            @Override
            public void push(ApnsNotification message) throws NetworkIOException {
                logWarning();
            }

            private void logWarning() {
                LOGGER.warn("Push will not be send, APNs not configured properly");
            }

            private <T> T logWarningAndReturn(T valueToReturn) {
                logWarning();
                return valueToReturn;
            }

            @Override
            public void start() {
            }

            @Override
            public void stop() {
            }

            @Override
            public Map<String, Date> getInactiveDevices() throws NetworkIOException {
                LOGGER.warn("Inactive devices not recoverable, APNs not configured properly");
                return Collections.emptyMap();
            }

            @Override
            public void testConnection() throws NetworkIOException {
                LOGGER.warn("APNs connection not tested, APNs not configured properly");
            }
        };
    }
}
