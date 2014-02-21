package be.appfoundry.promtius;

import java.util.List;

/**
 * This interface defines methods needed to persist and lookup client tokens.
 *
 * @param <T> The type of the token send by a client.
 * @param <P> The client platform type of the token.
 * @author Mike Seghers
 */
public interface ClientTokenService<T, P> {
    /**
     * searches client tokens for a given platform.
     */
    List<ClientToken<T, P>> findClientTokensForOperatingSystem(P platform);

    /**
     * Registers a client token.
     */
    void registerClientToken(ClientToken<T, P> clientToken);

    /**
     * Unregister a client token.
     */
    void unregisterClientToken(ClientToken<T, P> clientToken);
}
