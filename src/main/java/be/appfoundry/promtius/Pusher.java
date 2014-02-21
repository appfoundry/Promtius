package be.appfoundry.promtius;

/**
 * A pusher is capable of pushing a {@link PushPayload} to a specific client platform.
 *
 * @param <P> The client platform identifier type, identifying the platform to which the pusher pushes messages.
 * @author Mike Seghers
 */
public interface Pusher<P> {
    /**
     * Sends the given {@link PushPayload} to a specific client platform.
     */
    void sendPush(PushPayload payload);
}
