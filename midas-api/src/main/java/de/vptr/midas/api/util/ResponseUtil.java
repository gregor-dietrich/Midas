package de.vptr.midas.api.util;

import java.util.Optional;

import jakarta.ws.rs.core.Response;

/** ResponseUtil class. */
public class ResponseUtil {
    private ResponseUtil() {
    }

    /**
     * Returns a 201 Created response with the given entity, or 201 with no body if
     * entity is null.
     */
    public static <T> Response created(final T entity) {
        return entity == null
                ? Response.status(Response.Status.CREATED).build()
                : Response.status(Response.Status.CREATED).entity(entity).build();
    }

    public static Response noContent() {
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    public static Response notFound() {
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    public static Response ok() {
        return Response.status(Response.Status.OK).build();
    }

    public static <T> Response ok(final T entity) {
        return entity == null ? noContent() : Response.status(Response.Status.OK).entity(entity).build();
    }

    /** Processes the request and returns the result. */
    public static <T> Response okOrNotFound(final Optional<T> entity) {
        return entity.map(Response::ok)
                .map(Response.ResponseBuilder::build)
                .orElse(notFound());
    }

    public static Response unauthorized() {
        return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    public static Response forbidden() {
        return Response.status(Response.Status.FORBIDDEN).build();
    }

    // JAX-RS Response.Status does not include 422 UNPROCESSABLE_ENTITY
    public static Response unprocessable(final Object entity) {
        return Response.status(422).entity(entity).build();
    }
}
