package be.appfoundry.promtius;

import java.util.List;

/**
 * @author Mike Seghers
 */
public interface ClientTokenService<T> {
    List<ClientToken<T>> findClientTokensForOperatingSystem(ClientTokenType clientTokenType);
    void registerClientToken(ClientToken<T> clientToken);
    void unregisterClientToken(ClientToken<T> clientToken);
}
