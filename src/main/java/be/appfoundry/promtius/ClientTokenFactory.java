package be.appfoundry.promtius;

/**
 * @author Mike Seghers
 */
public interface ClientTokenFactory<T, P> {
    ClientToken<T, P> createClientToken(T token, P platform);
}
