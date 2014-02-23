package be.appfoundry.promtius;

/**
 * A Push aggregator typically pushes messages via an aggregate of different pushers. Use a push aggregator in your code instead of different {@link
 * be.appfoundry.promtius.Pusher}s. An aggregator will do the heavy lifting for you.
 *
 * @author Mike Seghers
 */
public interface PushAggregator {
    /**
     * Send the payload to the registered clients. Get notified on the progress in the given callback.
     */
    void sendPush(PushPayload payload, PusherAggregatorTracker callback);
}
