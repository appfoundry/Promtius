package be.appfoundry.promtius;

import com.google.common.base.Optional;

/**
 * @author Mike Seghers
 */
public class PushPayload {

    public static final String DEFAULT_SOUND_VALUE = "default";
    private String message;
    private String sound;

    /**
     * @deprecated use the {@link be.appfoundry.promtius.PushPayload.Builder} to create new instances instead!
     */
    @Deprecated
    public PushPayload(final String message) {
        this.message = message;
    }

    private PushPayload() {
    }

    public String getMessage() {
        return message;
    }

    public String getSound() {
        return sound;
    }

    public static class Builder {
        private Optional<String> message = Optional.absent();
        private Optional<String> sound = Optional.absent();

        public Builder withMessage(final String message) {
            this.message = Optional.of(message);
            return this;
        }

        public Builder withSound(final String sound) {
            this.sound = Optional.of(sound);
            return this;
        }

        public PushPayload build() {
            PushPayload pushPayload = new PushPayload();
            pushPayload.message = this.message.get();
            pushPayload.sound = this.sound.or(DEFAULT_SOUND_VALUE);
            return pushPayload;
        }
    }
}
