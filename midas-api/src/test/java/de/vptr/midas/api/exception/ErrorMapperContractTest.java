package de.vptr.midas.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParseException;

import jakarta.validation.ValidationException;

// NOTE: This is an intentional plain JUnit test, not a @QuarkusTest.
// It tests mapper contract logic via direct instantiation without requiring the full CDI container.
class ErrorMapperContractTest {

    private final ValidationExceptionMapper validationExceptionMapper = new ValidationExceptionMapper();
    private final JsonParseExceptionMapper jsonParseExceptionMapper = new JsonParseExceptionMapper();

    @Test
    void validationExceptionMapper_shouldReturnStructuredPayload() {
        final var response = this.validationExceptionMapper.toResponse(new ValidationException("Email invalid"));
        final var body = (ApiErrorResponse) response.getEntity();

        assertNotNull(body);
        assertEquals(422, response.getStatus());
        assertEquals(422, body.status());
        assertEquals("Validation failed", body.error());
        assertEquals("Email invalid", body.message());
    }

    @Test
    void validationExceptionMapper_shouldFallbackMessageWhenBlank() {
        final var response = this.validationExceptionMapper.toResponse(new ValidationException("   "));
        final var body = (ApiErrorResponse) response.getEntity();

        assertNotNull(body);
        assertEquals("Validation failed", body.error());
        assertEquals("Validation failed", body.message());
    }

    @Test
    void jsonParseExceptionMapper_shouldNotLeakParserDetails() {
        final var response = this.jsonParseExceptionMapper.toResponse(new JsonParseException(null, "unexpected token"));
        final var body = (ApiErrorResponse) response.getEntity();

        assertNotNull(body);
        assertEquals(400, response.getStatus());
        assertEquals(400, body.status());
        assertEquals("Bad Request", body.error());
        assertEquals("Malformed JSON request body", body.message());
    }
}
