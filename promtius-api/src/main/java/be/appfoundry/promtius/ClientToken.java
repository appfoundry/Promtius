package be.appfoundry.promtius;

/**
 * A client token represents a token send by a mobile device to identify itself. It holds this token, and its specific type identifying the platform it belongs to.
 *
 * @param <T> The type of the token send by the client.
 * @param <P> The platform identifier type.
 * @author Mike Seghers
 */
public interface ClientToken<T, P> {

    /**
     * @return the token as passed by the client
     */
    T getToken();

    /**
     * @return the platform the token belongs to.
     */
    P getPlatform();
}
