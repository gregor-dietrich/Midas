package de.vptr.midas.gui.util;

import jakarta.ws.rs.core.Response;

/**
 * Utility class for creating and handling Response objects in tests
 */
public final class ResponseTestUtil {

    private ResponseTestUtil() {
        // Utility class
    }

    /**
     * Create a mock Response with specific status
     */
    public static Response createMockResponse(final int status) {
        return Response.status(status).build();
    }

    /**
     * Create a mock Response with status and entity
     */
    public static <T> Response createMockResponse(final int status, final T entity) {
        return Response.status(status).entity(entity).build();
    }

    /**
     * Create a mock Response with status and entity class
     */
    public static <T> Response createMockResponse(final int status, final T entity, final Class<T> entityClass) {
        return Response.status(status).entity(entity).build();
    }

    /**
     * Create a 200 OK response with entity
     */
    public static <T> Response createOkResponse(final T entity) {
        return createMockResponse(200, entity);
    }

    /**
     * Create a 200 OK response without entity
     */
    public static Response createOkResponse() {
        return Response.status(200).build();
    }

    /**
     * Create a 201 Created response with entity
     */
    public static <T> Response createCreatedResponse(final T entity) {
        return createMockResponse(201, entity);
    }

    /**
     * Create a 404 Not Found response
     */
    public static Response createNotFoundResponse() {
        return createMockResponse(404);
    }

    /**
     * Create a 400 Bad Request response
     */
    public static Response createBadRequestResponse() {
        return createMockResponse(400);
    }

    /**
     * Create a 401 Unauthorized response
     */
    public static Response createUnauthorizedResponse() {
        return createMockResponse(401);
    }

    /**
     * Create a 500 Internal Server Error response
     */
    public static Response createServerErrorResponse() {
        return createMockResponse(500);
    }

    /**
     * Create a 500 Internal Server Error response
     */
    public static Response createInternalServerErrorResponse() {
        return Response.status(500).build();
    }

    /**
     * Create a 204 No Content response
     */
    public static Response createNoContentResponse() {
        return Response.status(204).build();
    }

}
