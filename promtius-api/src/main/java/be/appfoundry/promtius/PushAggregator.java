package be.appfoundry.promtius;

import java.util.Collection;

/**
 * A Push aggregator typically pushes messages via an aggregate of different pushers. Use a push aggregator in your code instead of different {@link
 * be.appfoundry.promtius.Pusher}s. An aggregator will do the heavy lifting for you.
 *
 * @param <P> The platform identifier type, identifying the platform to which the pusher pushes its messages.
 * @param <G> The type of the group identifier. A group identifier is used to put client tokens in a collection of groups, so that a push can be done to specific groups.
 * @author Mike Seghers
 */
public interface PushAggregator<P, G> {
    /**
     * Send the payload to the registered clients. Get notified on the progress in the given callback.
     */
    void sendPush(PushPayload payload, PusherAggregatorTracker callback);

    /**
     * Send the payload to the registered clients within the specified group. Get notified on the progress in the given callback.
     */
    void sendPush(PushPayload payload, Collection<G> groups, PusherAggregatorTracker callback);
}
