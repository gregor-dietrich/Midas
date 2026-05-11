package de.vptr.midas.gui.util;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.VaadinSession;

import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.view.BackendErrorView;
import de.vptr.midas.gui.view.LoginView;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.ProcessingException;

/** Handler for error events. */
public final class MidasErrorHandler {

    private static final Logger LOG = Logger.getLogger(MidasErrorHandler.class);

    private MidasErrorHandler() {
    }

    /**
     * Centralized async error handler. Unwraps CompletionException, then dispatches to auth/service/generic handling on
     * the UI thread.
     */
    public static void handle(final Throwable throwable, final UI ui) {
        final Throwable cause = unwrapAsyncThrowable(throwable);

        final var _ = ui.access(() -> dispatch(cause, ui));
    }

    /**
     * Overload for use inside an existing ui.access() block — skips wrapping.
     */
    public static void handleInUiContext(final Throwable throwable) {
        final Throwable cause = unwrapAsyncThrowable(throwable);
        dispatch(cause, UI.getCurrent());
    }

    /** Unwraps async wrapper exceptions. */
    public static Throwable unwrapAsyncThrowable(final Throwable throwable) {
        Throwable rootCause = throwable;
        while ((rootCause instanceof CompletionException || rootCause instanceof ExecutionException)
                && rootCause.getCause() != null) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

    /** Dispatches detached async errors through session handler or logs them. */
    public static void handleDetached(final Throwable throwable, final VaadinSession session) {
        final Throwable cause = unwrapAsyncThrowable(throwable);

        if (session != null && session.getErrorHandler() != null) {
            try {
                session.getErrorHandler().error(new ErrorEvent(cause));
                return;
            } catch (final RuntimeException e) {
                LOG.error("Failed to dispatch detached async error to Vaadin session handler", e);
            }
        }

        logDetached(cause);
    }

    /** Logs exception details and shows a sanitized notification. */
    public static void showSanitizedError(@Nullable final Throwable throwable, final String fallbackMessage) {
        final Throwable cause = throwable == null ? null : unwrapAsyncThrowable(throwable);
        logUserFacing(cause, fallbackMessage);
        NotificationUtil.showError(resolveUserMessage(cause, fallbackMessage));
    }

    private static void dispatch(@Nullable final Throwable cause, final UI ui) {
        if (cause instanceof AuthenticationException) {
            LOG.warn("Authentication error, redirecting to login", cause);
            // Navigate first — notification is secondary and must not block the redirect.
            ui.navigate(LoginView.class);
            NotificationUtil.showError("Session expired. Please log in again.");
        } else if (cause instanceof TimeoutException) {
            LOG.warn("Async operation timed out", cause);
            NotificationUtil.showError("Operation timed out. Please try again.");
        } else if (cause instanceof ServiceException && isConnectionFailure(cause)) {
            LOG.error("Backend connection failed, redirecting to error page", cause);
            ui.navigate(BackendErrorView.class);
        } else if (cause instanceof ServiceException) {
            LOG.error("Service error during async operation", cause);
            NotificationUtil.showError("Operation failed. Please try again.");
        } else {
            LOG.error("Unexpected error during async operation", cause);
            NotificationUtil.showError("An unexpected error occurred");
        }
    }

    private static boolean isConnectionFailure(final Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ProcessingException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static void logDetached(final Throwable cause) {
        if (cause instanceof TimeoutException) {
            LOG.warn("Detached async operation timed out", cause);
        } else if (cause instanceof AuthenticationException) {
            LOG.warn("Detached async authentication failure", cause);
        } else if (cause instanceof ServiceException) {
            LOG.error("Detached async service failure", cause);
        } else {
            LOG.error("Detached async operation failed", cause);
        }
    }

    static String resolveUserMessage(final Throwable cause, final String fallbackMessage) {
        if (cause instanceof AuthenticationException) {
            return "Session expired. Please log in again.";
        }
        if (cause instanceof TimeoutException) {
            return "Operation timed out. Please try again.";
        }
        if (fallbackMessage != null && !fallbackMessage.isBlank()) {
            return fallbackMessage;
        }
        if (cause instanceof ServiceException) {
            return "Operation failed. Please try again.";
        }
        return "An unexpected error occurred";
    }

    private static void logUserFacing(final Throwable cause, final String fallbackMessage) {
        final String message = resolveUserMessage(cause, fallbackMessage);
        if (cause instanceof AuthenticationException) {
            LOG.warnf(cause, "Authentication failure shown to user: %s", message);
        } else if (cause instanceof TimeoutException) {
            LOG.warnf(cause, "Timeout shown to user: %s", message);
        } else if (cause instanceof ServiceException) {
            LOG.errorf(cause, "Service failure shown to user: %s", message);
        } else if (cause != null) {
            LOG.errorf(cause, "Unexpected failure shown to user: %s", message);
        } else {
            LOG.errorf("Unknown failure shown to user: %s", message);
        }
    }
}
