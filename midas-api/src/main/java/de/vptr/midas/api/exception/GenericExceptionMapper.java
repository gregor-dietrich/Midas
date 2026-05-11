package de.vptr.midas.api.exception;

import org.hibernate.exception.ConstraintViolationException;
import org.jboss.logging.Logger;

import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

// Note: ExceptionMapper<Exception> intentionally excludes Error subclasses
// (OOM, SOE) which should propagate to the container.
/** Catch-all mapper that prevents stack trace leaks to API clients. */
@Provider
@Priority(Priorities.USER + 1000)
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(GenericExceptionMapper.class);
    private static final String GENERIC_INTERNAL_MESSAGE = "An unexpected error occurred";

    @Context
    @Nullable
    UriInfo uriInfo;

    @Override
    public Response toResponse(final Exception exception) {
        if (exception instanceof final WebApplicationException wae) {
            return mapWebApplicationException(wae);
        }

        if (isCausedBy(exception, ConstraintViolationException.class)) {
            LOG.warnf("Constraint violation: %s", exception.getMessage());
            return buildErrorResponse(409, "Conflict", "A resource with the same identifier already exists");
        }

        LOG.errorf(exception, "Unhandled exception on %s %s: %s", uriInfo != null ? uriInfo.getRequestUri() : "unknown",
                exception.getClass().getSimpleName(), exception.getMessage());

        return buildErrorResponse(500, "Internal Server Error", GENERIC_INTERNAL_MESSAGE);
    }

    private static boolean isCausedBy(final Throwable exception, final Class<? extends Throwable> type) {
        Throwable cause = exception;
        while (cause != null) {
            if (type.isInstance(cause)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private static Response mapWebApplicationException(final WebApplicationException exception) {
        final Response response = exception.getResponse();
        final int status = response != null ? response.getStatus() : 500;
        final String reason = resolveReason(status);
        if (status >= 500) {
            return buildErrorResponse(status, reason, GENERIC_INTERNAL_MESSAGE);
        }

        final String message = resolveClientMessage(exception.getMessage(), status, reason);
        return buildErrorResponse(status, reason, message);
    }

    private static String resolveClientMessage(@Nullable final String rawMessage, final int status,
            final String fallback) {
        if (rawMessage == null) {
            return fallback;
        }

        final String message = rawMessage.trim();
        if (message.isEmpty() || message.equalsIgnoreCase(fallback)
                || message.startsWith(WebApplicationException.class.getName()) || message.contains("HTTP " + status)) {
            return fallback;
        }

        return message;
    }

    private static String resolveReason(final int status) {
        final Response.Status statusType = Response.Status.fromStatusCode(status);
        return statusType != null ? statusType.getReasonPhrase() : "HTTP " + status;
    }

    private static Response buildErrorResponse(final int status, final String error, final String message) {
        return Response.status(status).entity(new ApiErrorResponse(status, error, message)).build();
    }
}
