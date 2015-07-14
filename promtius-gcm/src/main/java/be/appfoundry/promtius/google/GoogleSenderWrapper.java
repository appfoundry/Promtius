package be.appfoundry.promtius.google;

import be.appfoundry.custom.google.android.gcm.server.Message;
import be.appfoundry.custom.google.android.gcm.server.MulticastResult;

import java.io.IOException;
import java.util.List;

/**
 * @author Mike Seghers
 */
public interface GoogleSenderWrapper {
    MulticastResult send(Message message, List<String> deviceRegistrationIds, int numberOfRetries) throws IOException;
}
