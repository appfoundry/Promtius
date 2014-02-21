package be.appfoundry.promtius;

/**
 * @author Mike Seghers
 */
public interface Pusher {
    void sendPush(PushPayload payload);
}
