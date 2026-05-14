package de.vptr.midas.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.SQLException;

import org.hibernate.exception.ConstraintViolationException;
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

    @Test
    void testToResponse_returnsConflict_whenConstraintViolationExceptionInCause() {
        final var constraintEx = new ConstraintViolationException("constraint", new SQLException(), "constraint_name");
        final var wrappedException = new RuntimeException("wrap", constraintEx);

        final Response response = mapper.toResponse(wrappedException);

        final var body = (ApiErrorResponse) response.getEntity();
        assertEquals(409, response.getStatus());
        assertEquals("Conflict", body.error());
        assertEquals("A resource with the same identifier already exists", body.message());
    }

    @Test
    void testToResponse_returnsConflict_whenConstraintViolationExceptionDeepInCauseChain() {
        final var constraintEx = new ConstraintViolationException("constraint", new SQLException(), "constraint_name");
        final var inner = new RuntimeException("inner", constraintEx);
        final var outer = new RuntimeException("outer", inner);

        final Response response = mapper.toResponse(outer);

        assertEquals(409, response.getStatus());
    }

    @Test
    void testToResponse_4xx_withDefaultHttpStatusMessage_returnsFallback() {
        // No message arg → getMessage() = "HTTP 400 Bad Request" which matches contains("HTTP 400")
        final Response response = mapper.toResponse(new WebApplicationException(Response.Status.BAD_REQUEST));

        final var body = (ApiErrorResponse) response.getEntity();
        assertEquals(400, response.getStatus());
        assertEquals("Bad Request", body.message());
    }

    @Test
    void testToResponse_4xx_withUnknownStatusCode_usesHttpFallbackReason() {
        // Status 499 is not in Response.Status enum → resolveReason returns "HTTP 499"
        final Response response = mapper.toResponse(new WebApplicationException(Response.status(499).build()));

        assertEquals(499, response.getStatus());
        final var body = (ApiErrorResponse) response.getEntity();
        assertEquals("HTTP 499", body.error());
    }

    @Test
    void testToResponse_4xx_withCaseInsensitiveMatchToFallback_returnsFallback() {
        // "BAD REQUEST".equalsIgnoreCase("Bad Request") → true → returns fallback
        final Response response =
                mapper.toResponse(new WebApplicationException("BAD REQUEST", Response.Status.BAD_REQUEST));

        final var body = (ApiErrorResponse) response.getEntity();
        assertEquals(400, response.getStatus());
        assertEquals("Bad Request", body.message());
    }

    @Test
    void testToResponse_4xx_withBlankMessageAfterTrim_returnsFallback() {
        // " ".trim() == "" → isEmpty() true → returns fallback
        final Response response = mapper.toResponse(new WebApplicationException("   ", Response.Status.BAD_REQUEST));

        final var body = (ApiErrorResponse) response.getEntity();
        assertEquals(400, response.getStatus());
        assertEquals("Bad Request", body.message());
    }

    @Test
    void testToResponse_4xx_withWaeClassNamePrefix_returnsFallback() {
        // message starts with WAE FQCN → startsWith() true → returns fallback
        final String msg = WebApplicationException.class.getName() + ": something unexpected";
        final Response response = mapper.toResponse(new WebApplicationException(msg, Response.Status.BAD_REQUEST));

        final var body = (ApiErrorResponse) response.getEntity();
        assertEquals(400, response.getStatus());
        assertEquals("Bad Request", body.message());
    }

    @Test
    @SuppressWarnings("NullAway")
    void testToResponse_4xx_withNullMessage_returnsFallback() {
        // getMessage() returns null → rawMessage == null → returns fallback
        final var wae = new WebApplicationException(Response.Status.BAD_REQUEST) {
            @Override
            public String getMessage() {
                return null;
            }
        };
        final Response response = mapper.toResponse(wae);

        final var body = (ApiErrorResponse) response.getEntity();
        assertEquals(400, response.getStatus());
        assertEquals("Bad Request", body.message());
    }

    @Test
    @SuppressWarnings("NullAway")
    void testToResponse_withNullResponse_returns500() {
        // getResponse() returns null → status defaults to 500 → masks as internal error
        final var wae = new WebApplicationException() {
            @Override
            public Response getResponse() {
                return null;
            }
        };
        final Response response = mapper.toResponse(wae);

        assertEquals(500, response.getStatus());
    }
}
