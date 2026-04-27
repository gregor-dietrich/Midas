package de.vptr.midas.gui.exception;

/** Exception for authentication errors. */
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(final String message) {
        super(message);
    }

    public AuthenticationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
