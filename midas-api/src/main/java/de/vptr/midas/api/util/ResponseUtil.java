package de.vptr.midas.api.util;

import java.util.Optional;

import de.vptr.midas.api.exception.ApiErrorResponse;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.core.Response;

/** ResponseUtil class. */
public final class ResponseUtil {
    private ResponseUtil() {
    }

    /**
     * Returns a 201 Created response with the given entity, or 201 with no body if entity is null.
     */
    public static <T> Response created(@Nullable final T entity) {
        return entity == null ? Response.status(Response.Status.CREATED).build()
                : Response.status(Response.Status.CREATED).entity(entity).build();
    }

    /**
     * Returns a 204 No Content response. Use this for successful deletions or empty results.
     */
    public static Response noContent() {
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    /** Returns 404 Not Found with a structured error body. */
    public static Response notFound() {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ApiErrorResponse(404, "Not Found", "The requested resource was not found.")).build();
    }

    /**
     * Returns a 200 OK response with no body. Use this for simple success acknowledgements.
     */
    public static Response ok() {
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Returns 200 OK with the entity body, or 204 No Content if entity is null.
     */
    public static <T> Response ok(@Nullable final T entity) {
        return entity == null ? noContent() : Response.status(Response.Status.OK).entity(entity).build();
    }

    /**
     * Returns 200 OK with the entity if present, or 404 Not Found with a structured error body if absent.
     */
    public static <T> Response okOrNotFound(final Optional<T> entity) {
        return entity.map(Response::ok).map(Response.ResponseBuilder::build).orElse(notFound());
    }

    /**
     * Returns a 401 Unauthorized response. Use this when authentication is required but missing.
     */
    public static Response unauthorized() {
        return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    /**
     * Returns a 403 Forbidden response. Use this when the user lacks permission for the action.
     */
    public static Response forbidden() {
        return Response.status(Response.Status.FORBIDDEN).build();
    }

    /**
     * Returns a 422 Unprocessable Entity response with the given entity body. Note that JAX-RS Response.Status does not
     * include 422 UNPROCESSABLE_ENTITY.
     */
    public static Response unprocessable(final Object entity) {
        return Response.status(422).entity(entity).build();
    }
}
