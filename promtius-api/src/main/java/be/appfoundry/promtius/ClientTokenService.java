package be.appfoundry.promtius;

import java.util.Collection;
import java.util.List;

/**
 * This interface defines methods needed to persist and lookup client tokens.
 *
 * @param <CT> The type of the ClientToken
 * @param <T> The type of the token send by a client.
 * @param <P> The platform identifier type.
 * @param <G> The type of the group identifier. A group identifier is used to put client tokens in a collection of groups, so that a push can be done to specific groups.
 * @author Mike Seghers
 */
public interface ClientTokenService<CT extends ClientToken<T, P>, T, P, G> {
    /**
     * searches client tokens for a given platform.
     */
    List<CT> findClientTokensForOperatingSystem(P platform);

    /**
     * Unregister a client token.
     */
    void unregisterClientToken(CT clientToken);

    List<CT> findClientTokensForOperatingSystem(P platform, Collection<G> groups);

    /**
     * Replace the existing token's token value with the given token value.
     */
    void changeClientToken(CT clientToken, T newTokenValue);
}
