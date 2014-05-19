package be.appfoundry.promtius;

/**
 * This factory can create {@link ClientToken}s. This factory is typically used by {@link be.appfoundry.promtius.Pusher}s to create tokens when providing feedback to a {@link
 * ClientTokenService}.
 *
 * @param <CT> The type of ClientTokens this factory is creating
 * @param <T> The type of the token send by the client.
 * @param <P> The platform identifier type.
 * @author Mike Seghers
 */
public interface ClientTokenFactory<CT extends ClientToken<T, P>, T, P> {
    /**
     * Create a ClientToken with the given parameters.
     */
    CT createClientToken(T token, P platform);
}
