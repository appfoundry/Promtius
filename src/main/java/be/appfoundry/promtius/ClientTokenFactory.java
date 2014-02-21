package be.appfoundry.promtius;

/**
 * @author Mike Seghers
 */
public interface ClientTokenFactory<T> {
    ClientToken<T> createClientToken(T token, ClientTokenType clientTokenType);
}
