package be.appfoundry.promtius.google;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Sender;

import java.io.IOException;
import java.util.List;

/**
 * @author Mike Seghers
 */
public class GoogleSenderWrapperDefaultImpl implements GoogleSenderWrapper {
    private Sender sender;

    public GoogleSenderWrapperDefaultImpl(final Sender sender) {
        this.sender = sender;
    }

    @Override
    public MulticastResult send(Message message, List<String> deviceRegistrationIds, int numberOfRetries) throws IOException {
        return sender.send(message, deviceRegistrationIds, numberOfRetries);
    }
}
