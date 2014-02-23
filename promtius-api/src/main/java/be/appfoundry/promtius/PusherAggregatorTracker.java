package be.appfoundry.promtius;

/**
 * A tracker can be used by an aggregator to track it's progress while sending payload to registered clients. When the aggregator is finished, it can report this by calling the
 * markFinished method.
 *
 * @author Mike Seghers
 */
public interface PusherAggregatorTracker {
    /**
     * Called by an aggregator when it has finished sending payload to registered clients.
     */
    void markFinished();
}
