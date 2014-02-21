package be.appfoundry.promtius;

/**
 * @author Mike Seghers
 */
public interface ClientToken<T> {

    T getToken();

    ClientTokenType getClientTokenType();
}
