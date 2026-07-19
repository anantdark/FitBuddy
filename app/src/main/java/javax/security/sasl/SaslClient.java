package javax.security.sasl;

/**
 * Minimal Android stub — the platform does not ship {@code javax.security.sasl}.
 * MongoDB's SCRAM authenticator implements this interface; only the type must exist.
 */
public interface SaslClient {
    boolean hasInitialResponse();

    byte[] evaluateChallenge(byte[] challenge) throws SaslException;

    boolean isComplete();

    String getMechanismName();

    byte[] unwrap(byte[] incoming, int offset, int len) throws SaslException;

    byte[] wrap(byte[] outgoing, int offset, int len) throws SaslException;

    Object getNegotiatedProperty(String propName);

    void dispose() throws SaslException;
}
