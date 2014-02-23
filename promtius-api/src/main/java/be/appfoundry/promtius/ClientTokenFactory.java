package be.appfoundry.promtius;

/**
 * This factory can create {@link ClientToken}s. This factory is typically used by {@link be.appfoundry.promtius.Pusher}s to create tokens when providing feedback to a {@link
 * ClientTokenService}.
 *
 * @param <T> The type of the token send by the client.
 * @param <P> The client platform type of the token.
 * @author Mike Seghers
 */
public interface ClientTokenFactory<T, P> {
    /**
     * Create a ClientToken with the given parameters.
     */
    ClientToken<T, P> createClientToken(T token, P platform);
}
