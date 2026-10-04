package de.vptr.midas.gui.util;

import java.util.Optional;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.AuthService;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Centralises the duplicated try/catch exception-handling pattern found in every GUI service method that calls a REST
 * client.
 *
 * <p>
 * The REST clients return typed entities and signal every 4xx/5xx as a {@link WebApplicationException}, which is
 * translated here:
 * <ul>
 * <li>401 logs the session out and becomes an {@link AuthenticationException};</li>
 * <li>404 becomes {@link Optional#empty()} for {@link #executeOptional} and the caller's message for
 * {@link #executeVoid(Runnable, AuthService, Logger, String, String)};</li>
 * <li>422 carries the backend's validation message, behind the caller's prefix when one is given;</li>
 * <li>anything else becomes a generic {@link ServiceException}.</li>
 * </ul>
 */
public final class ServiceHelper {

    private static final int NOT_FOUND = Response.Status.NOT_FOUND.getStatusCode();

    private ServiceHelper() {
        // utility class
    }

    /**
     * Executes a value-returning REST client call with standardised exception handling.
     *
     * @param action
     *            the supplier wrapping the REST call
     * @param authService
     *            used to invalidate the session on 401
     * @param log
     *            caller's logger for contextual messages
     * @param operation
     *            short description for log messages (e.g. "fetching pages")
     * @return the value produced by {@code action}
     */
    public static <T> T execute(final Supplier<T> action, final AuthService authService, final Logger log,
            final String operation) {
        return execute(action, authService, log, operation, null);
    }

    /**
     * Executes a value-returning REST client call, prefixing a 422 validation message with {@code failurePrefix}.
     *
     * @param action
     *            the supplier wrapping the REST call
     * @param authService
     *            used to invalidate the session on 401
     * @param log
     *            caller's logger for contextual messages
     * @param operation
     *            short description for log messages
     * @param failurePrefix
     *            prefix for the backend's validation message (e.g. "Failed to create page"), or null for none
     * @return the value produced by {@code action}
     */
    public static <T> T execute(final Supplier<T> action, final AuthService authService, final Logger log,
            final String operation, @Nullable final String failurePrefix) {
        try {
            return action.get();
        } catch (final ProcessingException e) {
            log.errorf(e, "Connection error while %s", operation);
            throw new ServiceException("Backend connection failed", e);
        } catch (final WebApplicationException e) {
            throw translateWebApplicationException(e, authService, log, operation, failurePrefix);
        } catch (final AuthenticationException | ServiceException e) {
            throw e;
        } catch (final RuntimeException e) {
            log.errorf(e, "Unexpected error while %s", operation);
            throw new ServiceException("Unexpected error", e);
        }
    }

    /**
     * Executes a lookup, returning empty when the backend answers 404.
     *
     * @param action
     *            the supplier wrapping the REST call
     * @param authService
     *            used to invalidate the session on 401
     * @param log
     *            caller's logger for contextual messages
     * @param operation
     *            short description for log messages
     * @return the value produced by {@code action}, or empty on 404
     */
    public static <T> Optional<T> executeOptional(final Supplier<T> action, final AuthService authService,
            final Logger log, final String operation) {
        return execute(() -> {
            try {
                return Optional.of(action.get());
            } catch (final WebApplicationException e) {
                if (!isNotFound(e)) {
                    throw e;
                }
                e.getResponse().close();
                log.debugf("Not found while %s", operation);
                return Optional.empty();
            }
        }, authService, log, operation);
    }

    /**
     * Executes a void REST client call with standardised exception handling.
     *
     * @param action
     *            the runnable wrapping the REST call
     * @param authService
     *            used to invalidate the session on 401
     * @param log
     *            caller's logger for contextual messages
     * @param operation
     *            short description for log messages
     */
    public static void executeVoid(final Runnable action, final AuthService authService, final Logger log,
            final String operation) {
        execute(() -> {
            action.run();
            return Boolean.TRUE;
        }, authService, log, operation);
    }

    /**
     * Executes a void REST client call on a single entity, throwing {@code notFoundMessage} when it does not exist.
     *
     * @param action
     *            the runnable wrapping the REST call
     * @param authService
     *            used to invalidate the session on 401
     * @param log
     *            caller's logger for contextual messages
     * @param operation
     *            short description for log messages
     * @param notFoundMessage
     *            message of the {@link ServiceException} thrown on 404 (e.g. "Page not found")
     */
    public static void executeVoid(final Runnable action, final AuthService authService, final Logger log,
            final String operation, final String notFoundMessage) {
        if (executeOptional(() -> {
            action.run();
            return Boolean.TRUE;
        }, authService, log, operation).isEmpty()) {
            throw new ServiceException(notFoundMessage);
        }
    }

    private static boolean isNotFound(final WebApplicationException exception) {
        final Response response = exception.getResponse();
        return response != null && response.getStatus() == NOT_FOUND;
    }

    private static RuntimeException translateWebApplicationException(final WebApplicationException exception,
            final AuthService authService, final Logger log, final String operation,
            @Nullable final String failurePrefix) {
        try (Response response = exception.getResponse()) {
            final String statusText = response == null ? "unknown" : Integer.toString(response.getStatus());
            final String backendMessage =
                    response == null ? "HTTP unknown" : ErrorMessageUtil.extractErrorMessage(response);
            log.warnf("HTTP error while %s: %s — backend message: %s", operation, statusText, backendMessage);
            if (response != null && response.getStatus() == 401) {
                authService.logout();
                return new AuthenticationException("Session expired", exception);
            }
            if (response != null && response.getStatus() == 422) {
                return new ServiceException(
                        failurePrefix != null && !failurePrefix.isBlank() ? failurePrefix + ": " + backendMessage
                                : "HTTP 422".equals(backendMessage) ? "Backend error: 422" : backendMessage,
                        exception);
            }
            return new ServiceException("Backend request failed", exception);
        }
    }
}
