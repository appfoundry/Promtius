package be.appfoundry.promtius;

import java.util.Collection;

/**
 * A pusher is capable of pushing a {@link PushPayload} to a specific client platform.
 *
 * @param <P> The platform identifier type, identifying the platform to which the pusher pushes its messages.
 * @param <G> The type of the group identifier. A group identifier is used to put client tokens in a collection of groups, so that a push can be done to specific groups.
 * @author Mike Seghers
 */
public interface Pusher<P, G> {
    /**
     * Sends the given {@link PushPayload} to a specific client platform.
     *
     * @throws be.appfoundry.promtius.exception.PushFailedException
     */
    void sendPush(PushPayload payload);

    /**
     * Sends the given {@link PushPayload} to a specific client platform and a specific group.
     *
     * @throws be.appfoundry.promtius.exception.PushFailedException
     */
    void sendPush(PushPayload payload, Collection<G> groups);

    /**
     * Returns the platform identifier for which this pusher is working.
     */
    P getPlatform();

}
