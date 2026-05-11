package de.vptr.midas.gui.exception;

import jakarta.annotation.Nullable;

/** Exception for service errors. */
public class ServiceException extends RuntimeException {
    public ServiceException(final String message) {
        super(message);
    }

    public ServiceException(final String message, @Nullable final Throwable cause) {
        super(message, cause);
    }
}
