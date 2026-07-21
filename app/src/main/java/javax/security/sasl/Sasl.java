package javax.security.sasl;

import java.util.Map;
import javax.security.auth.callback.CallbackHandler;

/**
 * Minimal Android stub. Atlas SCRAM uses MongoDB's own SaslClient impl and does not
 * call {@link #createSaslClient}; GSSAPI/PLAIN would need a real provider.
 */
public final class Sasl {
    private Sasl() {}

    public static SaslClient createSaslClient(
            String[] mechanisms,
            String authorizationId,
            String protocol,
            String serverName,
            Map<String, ?> props,
            CallbackHandler cbh
    ) throws SaslException {
        return null;
    }
}
