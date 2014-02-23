package be.appfoundry.promtius.apple;

import com.notnoop.apns.ApnsNotification;
import com.notnoop.apns.ApnsService;
import com.notnoop.apns.EnhancedApnsNotification;
import com.notnoop.exceptions.NetworkIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * @author Mike Seghers
 */
public final class NoopApnsService implements ApnsService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NoopApnsService.class);

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
}
