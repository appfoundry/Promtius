package be.appfoundry.promtius.apple;

import com.google.common.base.Strings;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import com.notnoop.apns.ApnsServiceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

/**
 * @author Mike Seghers
 */
public class ApnsServiceFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApnsServiceFactory.class);

    private ApnsServiceFactory() {
        throw new UnsupportedOperationException("You should not call the constructor of this class!");
    }

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
            URL resource = ApplePushNotificationServicePusher.class.getResource(certPath);
            if (resource != null) {
                LOGGER.info("APNs configuration seems ok, setting up APNs.");
                ApnsServiceBuilder apnsServiceBuilder = APNS.newService().withCert(resource.getPath(), certPass);
                if (sandbox) {
                    LOGGER.info("APNs setup for sandbox");
                    apnsServiceBuilder.withSandboxDestination();
                } else {
                    LOGGER.info("APNs setup for production");
                    apnsServiceBuilder.withProductionDestination();
                }
                service = apnsServiceBuilder.build();
            } else {
                LOGGER.warn(certPath + " could not be loaded, are you sure this is a valid resource path?");
                service = fallBackToNoOpService();
            }
        }
        return service;
    }

    private static ApnsService fallBackToNoOpService() {
        LOGGER.warn("APNs Service not configured properly, falling back to NO-OP service, your push notifications will not be send to Apple!");
        return new NoopApnsService();
    }
}
