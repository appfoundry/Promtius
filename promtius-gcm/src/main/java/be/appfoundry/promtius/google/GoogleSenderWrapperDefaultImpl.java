package be.appfoundry.promtius.google;

import be.appfoundry.custom.google.android.gcm.server.Message;
import be.appfoundry.custom.google.android.gcm.server.MulticastResult;
import be.appfoundry.custom.google.android.gcm.server.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * @author Mike Seghers
 */
public class GoogleSenderWrapperDefaultImpl implements GoogleSenderWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleSenderWrapperDefaultImpl.class);

    private Sender sender;

    public GoogleSenderWrapperDefaultImpl(final Sender sender) {
        this.sender = sender;
    }

    @Override
    public MulticastResult send(Message message, List<String> deviceRegistrationIds, int numberOfRetries) throws IOException {
        LOGGER.debug("sending message to {} ids, retrying {} times", deviceRegistrationIds.size(), numberOfRetries);
        return sender.send(message, deviceRegistrationIds, numberOfRetries);
    }
}
