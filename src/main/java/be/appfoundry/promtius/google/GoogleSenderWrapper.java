package be.appfoundry.promtius.google;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;

import java.io.IOException;
import java.util.List;

/**
 * @author Mike Seghers
 */
public interface GoogleSenderWrapper {
    MulticastResult send(Message message, List<String> deviceRegistrationIds, int numberOfRetries) throws IOException;
}
