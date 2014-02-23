package be.appfoundry.promtius;

import java.util.Collection;
import java.util.List;

/**
 * This interface defines methods needed to persist and lookup client tokens.
 *
 * @param <T> The type of the token send by a client.
 * @param <P> The platform identifier type.
 * @param <G> The type of the group identifier. A group identifier is used to put client tokens in a collection of groups, so that a push can be done to specific groups.
 * @author Mike Seghers
 */
public interface ClientTokenService<T, P, G> {
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

    List<ClientToken<T, P>> findClientTokensForOperatingSystem(P platform, Collection<G> groups);
}
