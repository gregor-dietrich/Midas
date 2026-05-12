package de.vptr.midas.gui.util;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.AuthService;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

/**
 * Centralises the duplicated try/catch exception-handling pattern found in every GUI service method that calls a REST
 * client.
 *
 * <p>
 * Three entry-points cover all current call shapes:
 * <ul>
 * <li>{@link #execute} – value-returning calls</li>
 * <li>{@link #executeVoid} – fire-and-forget calls</li>
 * <li>{@link #executeForResponse} – calls that return a raw {@link Response}</li>
 * </ul>
 */
public final class ServiceHelper {

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

    private static <T> T execute(final Supplier<T> action, final AuthService authService, final Logger log,
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
            return null;
        }, authService, log, operation);
    }

    /**
     * Executes a REST client call that returns a {@link Response}, with standardised exception handling. Useful for
     * methods that need to inspect status codes on the raw response.
     *
     * @param action
     *            the supplier wrapping the REST call
     * @param authService
     *            used to invalidate the session on 401
     * @param log
     *            caller's logger for contextual messages
     * @param operation
     *            short description for log messages
     * @return the {@link Response} produced by {@code action}
     */
    public static Response executeForResponse(final Supplier<Response> action, final AuthService authService,
            final Logger log, final String operation) {
        return execute(action, authService, log, operation);
    }

    /**
     * Executes a REST client call that returns a {@link Response}, closes it after processing and applies standardised
     * exception handling.
     *
     * @param action
     *            the supplier wrapping the REST call
     * @param authService
     *            used to invalidate the session on 401
     * @param log
     *            caller's logger for contextual messages
     * @param operation
     *            short description for log messages
     * @param responseHandler
     *            handler that inspects and parses the response
     * @return the value produced by {@code responseHandler}
     */
    public static <R> R executeWithResponse(final Supplier<Response> action, final AuthService authService,
            final Logger log, final String operation, final Function<Response, R> responseHandler) {
        return executeWithResponse(action, authService, log, operation, null, responseHandler);
    }

    /**
     * Executes a REST client call that returns a {@link Response}, closes it after processing and translates backend
     * failures with a caller-provided prefix.
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
     *            message prefix for backend error translation
     * @param responseHandler
     *            handler that inspects and parses the response
     * @return the value produced by {@code responseHandler}
     */
    public static <R> R executeWithResponse(final Supplier<Response> action, final AuthService authService,
            final Logger log, final String operation, @Nullable final String failurePrefix,
            final Function<Response, R> responseHandler) {
        return execute(() -> {
            try (Response response = action.get()) {
                handleUnauthorizedResponse(response, authService);
                return responseHandler.apply(response);
            }
        }, authService, log, operation, failurePrefix);
    }

    /**
     * Reads a typed entity from a response and translates client-side parsing failures to {@link ServiceException}.
     *
     * @param response
     *            response containing the entity
     * @param entityType
     *            target entity type
     * @param payloadDescription
     *            short payload description for diagnostics
     * @return parsed entity
     */
    public static <E> E readEntity(final Response response, final Class<E> entityType,
            final String payloadDescription) {
        try {
            return response.readEntity(entityType);
        } catch (final RuntimeException e) {
            throw new ServiceException("Failed to read " + payloadDescription, e);
        }
    }

    /**
     * Reads a generic entity from a response and translates client-side parsing failures to {@link ServiceException}.
     *
     * @param response
     *            response containing the entity
     * @param entityType
     *            target entity type
     * @param payloadDescription
     *            short payload description for diagnostics
     * @return parsed entity
     */
    public static <E> E readEntity(final Response response, final GenericType<E> entityType,
            final String payloadDescription) {
        try {
            return response.readEntity(entityType);
        } catch (final RuntimeException e) {
            throw new ServiceException("Failed to read " + payloadDescription, e);
        }
    }

    /**
     * Builds a {@link ServiceException} using the backend response body when available.
     *
     * @param failurePrefix
     *            caller-specific error prefix
     * @param response
     *            backend response
     * @return translated service exception
     */
    public static ServiceException responseFailure(final String failurePrefix, final Response response) {
        return new ServiceException(failurePrefix + ": " + ErrorMessageUtil.extractErrorMessage(response));
    }

    /**
     * Builds a {@link ServiceException} using the backend response body when available.
     *
     * @param failurePrefix
     *            caller-specific error prefix
     * @param response
     *            backend response
     * @param cause
     *            original cause
     * @return translated service exception
     */
    public static ServiceException responseFailure(final String failurePrefix, final Response response,
            @Nullable final Throwable cause) {
        return new ServiceException(failurePrefix + ": " + ErrorMessageUtil.extractErrorMessage(response), cause);
    }

    /**
     * Builds a generic backend failure exception while preserving structured error messages when present.
     *
     * @param response
     *            backend response
     * @return translated service exception
     */
    public static ServiceException backendError(final Response response) {
        return backendError(response, null);
    }

    /**
     * Builds a generic backend failure exception while preserving structured error messages when present.
     *
     * @param response
     *            backend response
     * @param cause
     *            original cause
     * @return translated service exception
     */
    public static ServiceException backendError(final Response response, @Nullable final Throwable cause) {
        final String message = backendErrorMessage(response);
        if (cause == null) {
            return new ServiceException(message);
        }
        return new ServiceException(message, cause);
    }

    /**
     * Reads an optional entity from a response, returning empty if status is 404, or parsing if 200. Throws
     * ServiceException if status is anything else.
     *
     * @param response
     *            response containing the entity
     * @param entityType
     *            target entity type
     * @param payloadDescription
     *            short payload description
     * @return optional parsed entity
     */
    public static <O> Optional<O> readOptionalEntity(final Response response, final Class<O> entityType,
            final String payloadDescription) {
        return readOptionalEntity(response, entityType, Function.identity(), payloadDescription);
    }

    /**
     * Reads an optional entity from a response, returning empty if status is 404, or parsing if 200. Throws
     * ServiceException if status is anything else.
     *
     * @param response
     *            response containing the entity
     * @param entityType
     *            target entity type
     * @param mapper
     *            function to map parsed entity to DTO
     * @param payloadDescription
     *            short payload description
     * @return optional mapped DTO
     */
    public static <R, T> Optional<T> readOptionalEntity(final Response response, final Class<R> entityType,
            final Function<R, T> mapper, final String payloadDescription) {
        if (response.getStatus() == 200) {
            final R responseDto = readEntity(response, entityType, payloadDescription);
            return Optional.of(mapper.apply(responseDto));
        } else if (response.getStatus() == 404) {
            return Optional.empty();
        } else {
            throw backendError(response);
        }
    }

    /**
     * Reads an entity from a response if it matches the expected status code, translating errors otherwise.
     *
     * @param response
     *            response containing the entity
     * @param expectedStatus
     *            expected HTTP status code (e.g. 200 or 201)
     * @param entityType
     *            target entity type
     * @param payloadDescription
     *            short payload description
     * @param failurePrefix
     *            message prefix on unexpected status
     * @return parsed entity
     */
    public static <S> S readExpectedStatusEntity(final Response response, final int expectedStatus,
            final Class<S> entityType, final String payloadDescription, final String failurePrefix) {
        return readExpectedStatusEntity(response, expectedStatus, entityType, Function.identity(), payloadDescription,
                failurePrefix);
    }

    /**
     * Reads an entity from a response if it matches the expected status code, translating errors otherwise.
     *
     * @param response
     *            response containing the entity
     * @param expectedStatus
     *            expected HTTP status code (e.g. 200 or 201)
     * @param entityType
     *            target entity type
     * @param mapper
     *            function to map parsed entity to DTO
     * @param payloadDescription
     *            short payload description
     * @param failurePrefix
     *            message prefix on unexpected status
     * @return mapped DTO
     */
    public static <R, T> T readExpectedStatusEntity(final Response response, final int expectedStatus,
            final Class<R> entityType, final Function<R, T> mapper, final String payloadDescription,
            final String failurePrefix) {
        if (response.getStatus() == expectedStatus) {
            final R responseDto = readEntity(response, entityType, payloadDescription);
            return mapper.apply(responseDto);
        }
        throw responseFailure(failurePrefix, response);
    }

    private static RuntimeException translateWebApplicationException(final WebApplicationException exception,
            final AuthService authService, final Logger log, final String operation,
            @Nullable final String failurePrefix) {
        try (Response response = exception.getResponse()) {
            final String statusText = response == null ? "unknown" : Integer.toString(response.getStatus());
            final String backendMessage = response == null ? null : ErrorMessageUtil.extractErrorMessage(response);
            log.warnf("HTTP error while %s: %s — backend message: %s", operation, statusText, backendMessage);
            if (response != null && response.getStatus() == 401) {
                authService.logout();
                return new AuthenticationException("Session expired", exception);
            }
            if (response != null && response.getStatus() == 422) {
                if (failurePrefix != null && !failurePrefix.isBlank()) {
                    return responseFailure(failurePrefix, response, exception);
                }
                return backendError(response, exception);
            }
            return new ServiceException("Backend request failed", exception);
        }
    }

    private static void handleUnauthorizedResponse(final Response response, final AuthService authService) {
        if (response != null && response.getStatus() == 401) {
            authService.logout();
            throw new AuthenticationException("Session expired");
        }
    }

    private static String backendErrorMessage(final Response response) {
        if (response == null) {
            return "Backend error";
        }
        final String extractedMessage = ErrorMessageUtil.extractErrorMessage(response);
        final String defaultStatusMessage = "HTTP " + response.getStatus();
        if (defaultStatusMessage.equals(extractedMessage)) {
            return "Backend error: " + response.getStatus();
        }
        return extractedMessage;
    }
}
