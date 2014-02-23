package be.appfoundry.promtius;

/**
 * @author Mike Seghers
 */
public class PushPayload {

    private String message;

    public PushPayload(final String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
