package de.vptr.midas.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

class GenericExceptionMapperTest {

    private final GenericExceptionMapper mapper = new GenericExceptionMapper();

    @Test
    void testToResponse_returns500() {
        final Response response = mapper.toResponse(new RuntimeException("secret details"));

        assertEquals(500, response.getStatus());
    }

    @Test
    void testToResponse_returnsGenericErrorBody() {
        final Response response = mapper.toResponse(new RuntimeException("secret details"));

        final var body = (ApiErrorResponse) response.getEntity();
        assertNotNull(body);
        assertEquals(500, body.status());
        assertEquals("Internal Server Error", body.error());
        assertEquals("An unexpected error occurred", body.message());
    }

    @Test
    void testToResponse_doesNotLeakExceptionDetails() {
        final Response response = mapper.toResponse(new RuntimeException("secret details"));

        final var body = (ApiErrorResponse) response.getEntity();
        assertNotNull(body);
        assertEquals("An unexpected error occurred", body.message());
    }

    @Test
    void testToResponse_wrapsClientWebApplicationException() {
        final Response response =
                mapper.toResponse(new WebApplicationException("Invalid query", Response.Status.BAD_REQUEST));

        final var body = (ApiErrorResponse) response.getEntity();
        assertNotNull(body);
        assertEquals(400, response.getStatus());
        assertEquals(400, body.status());
        assertEquals("Bad Request", body.error());
        assertEquals("Invalid query", body.message());
    }

    @Test
    void testToResponse_masksServerWebApplicationExceptionDetails() {
        final Response response =
                mapper.toResponse(new WebApplicationException("secret details", Response.Status.INTERNAL_SERVER_ERROR));

        final var body = (ApiErrorResponse) response.getEntity();
        assertNotNull(body);
        assertEquals(500, response.getStatus());
        assertEquals("Internal Server Error", body.error());
        assertEquals("An unexpected error occurred", body.message());
        assertNotEquals("secret details", body.message());
    }
}
