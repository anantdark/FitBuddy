package javax.security.sasl;

import java.io.IOException;

/** Minimal Android stub for MongoDB SCRAM auth (platform lacks javax.security.sasl). */
public class SaslException extends IOException {
    private static final long serialVersionUID = 1L;

    public SaslException() {
        super();
    }

    public SaslException(String message) {
        super(message);
    }

    public SaslException(String message, Throwable cause) {
        super(message, cause);
    }

    public SaslException(Throwable cause) {
        super(cause);
    }
}
