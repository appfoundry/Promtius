package be.appfoundry.promtius;

/**
 * @author Mike Seghers
 */
public interface PushAggregator {
    void sendPush(PushPayload payload, PusherAggregatorTracker callback);
}
