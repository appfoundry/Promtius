package be.appfoundry.promtius.exception;

/**
 * @author Mike Seghers
 */
public class PushFailedException extends PromtiusException {
    public PushFailedException() {
    }

    public PushFailedException(final String s) {
        super(s);
    }

    public PushFailedException(final String s, final Throwable throwable) {
        super(s, throwable);
    }

    public PushFailedException(final Throwable throwable) {
        super(throwable);
    }
}
