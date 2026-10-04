package de.vptr.midas.gui.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.ws.rs.core.Response;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("NullAway")
class ErrorMessageUtilTest {

    @Test
    void extractErrorMessage_shouldReturnParsedMessage_whenResponseContainsJsonMessage() {
        final var response = Mockito.mock(Response.class);
        when(response.hasEntity()).thenReturn(true);
        when(response.getStatus()).thenReturn(404);
        when(response.readEntity(String.class)).thenReturn("{\"message\":\"Not found\"}");

        final var result = ErrorMessageUtil.extractErrorMessage(response);

        assertThat(result).isEqualTo("Not found");
        Mockito.verify(response).bufferEntity();
    }

    @Test
    void extractErrorMessage_shouldFallbackToStatus_whenResponseContainsPlainText() {
        final var response = Mockito.mock(Response.class);
        when(response.hasEntity()).thenReturn(true);
        when(response.getStatus()).thenReturn(502);
        when(response.readEntity(String.class)).thenReturn("Bad gateway");

        final var result = ErrorMessageUtil.extractErrorMessage(response);

        assertThat(result).isEqualTo("HTTP 502");
    }

    @Test
    void extractErrorMessage_shouldFallbackToStatus_whenResponseContainsLongPlainText() {
        final var response = Mockito.mock(Response.class);
        when(response.hasEntity()).thenReturn(true);
        when(response.getStatus()).thenReturn(422);
        when(response.readEntity(String.class)).thenReturn("x".repeat(10_000));

        final var result = ErrorMessageUtil.extractErrorMessage(response);

        assertThat(result).isEqualTo("HTTP 422");
    }

    @Test
    void extractErrorMessage_shouldFallbackToStatus_whenJsonHasNoMessageField() {
        final var response = Mockito.mock(Response.class);
        when(response.hasEntity()).thenReturn(true);
        when(response.getStatus()).thenReturn(400);
        when(response.readEntity(String.class)).thenReturn("{\"code\":42}");

        final var result = ErrorMessageUtil.extractErrorMessage(response);

        assertThat(result).isEqualTo("HTTP 400");
    }

    @Test
    void extractErrorMessage_shouldReturnHttpStatus_whenBodyIsEmpty() {
        final var response = Mockito.mock(Response.class);
        when(response.hasEntity()).thenReturn(true);
        when(response.getStatus()).thenReturn(500);
        when(response.readEntity(String.class)).thenReturn("   ");

        final var result = ErrorMessageUtil.extractErrorMessage(response);

        assertThat(result).isEqualTo("HTTP 500");
    }

    @Test
    void extractErrorMessage_shouldReturnHttpStatus_whenBodyIsNull() {
        final var response = Mockito.mock(Response.class);
        when(response.hasEntity()).thenReturn(true);
        when(response.getStatus()).thenReturn(400);
        when(response.readEntity(String.class)).thenReturn(null);

        final var result = ErrorMessageUtil.extractErrorMessage(response);

        assertThat(result).isEqualTo("HTTP 400");
    }

    @Test
    void extractErrorMessage_shouldReturnHttpStatus_whenStatusIsNonError() {
        final var response = Mockito.mock(Response.class);
        when(response.hasEntity()).thenReturn(true);
        when(response.getStatus()).thenReturn(200);

        final var result = ErrorMessageUtil.extractErrorMessage(response);

        assertThat(result).isEqualTo("HTTP 200");
    }

    @Test
    void extractErrorMessage_shouldReturnHttpStatus_whenHasEntityIsFalse() {
        final var response = Mockito.mock(Response.class);
        when(response.hasEntity()).thenReturn(false);
        when(response.getStatus()).thenReturn(404);

        final var result = ErrorMessageUtil.extractErrorMessage(response);

        assertThat(result).isEqualTo("HTTP 404");
    }

    @Test
    void extractErrorMessage_shouldReturnHttpStatus_whenReadEntityThrowsException() {
        final var response = Mockito.mock(Response.class);
        when(response.hasEntity()).thenReturn(true);
        when(response.getStatus()).thenReturn(500);
        when(response.readEntity(String.class)).thenThrow(new IllegalStateException("Entity already consumed"));

        final var result = ErrorMessageUtil.extractErrorMessage(response);

        assertThat(result).isEqualTo("HTTP 500");
    }

    @Test
    void extractErrorMessage_shouldReturnParsedMessage_whenMessageContainsSpecialChars() {
        final var response = Mockito.mock(Response.class);
        when(response.hasEntity()).thenReturn(true);
        when(response.getStatus()).thenReturn(422);
        when(response.readEntity(String.class)).thenReturn("{\"message\":\"Field 'email' is invalid\"}");

        final var result = ErrorMessageUtil.extractErrorMessage(response);

        assertThat(result).isEqualTo("Field 'email' is invalid");
    }

    @Test
    void extractErrorMessage_shouldParseProblemDetailContract_whenMessageMissing() {
        final var response = Mockito.mock(Response.class);
        when(response.hasEntity()).thenReturn(true);
        when(response.getStatus()).thenReturn(400);
        when(response.readEntity(String.class)).thenReturn("{\"detail\":\"Invalid request payload\"}");

        final var result = ErrorMessageUtil.extractErrorMessage(response);

        assertThat(result).isEqualTo("Invalid request payload");
    }

    @Test
    void extractErrorMessage_shouldFallbackToStatus_whenBodyLooksLikeHtml() {
        final var response = Mockito.mock(Response.class);
        when(response.hasEntity()).thenReturn(true);
        when(response.getStatus()).thenReturn(500);
        when(response.readEntity(String.class)).thenReturn("<html><body>Server Error</body></html>");

        final var result = ErrorMessageUtil.extractErrorMessage(response);

        assertThat(result).isEqualTo("HTTP 500");
    }

    @Test
    void extractErrorMessage_shouldHandleNullResponse() {
        final var result = ErrorMessageUtil.extractErrorMessage(null);

        assertThat(result).isEqualTo("HTTP unknown");
    }

    private static Response errorResponse(final int status, final String body) {
        final var response = Mockito.mock(Response.class);
        when(response.hasEntity()).thenReturn(true);
        when(response.getStatus()).thenReturn(status);
        when(response.readEntity(String.class)).thenReturn(body);
        return response;
    }

    @ParameterizedTest
    @ValueSource(strings = { "\"text\"", "[1]", "{\"message\":42}", "{\"message\":\"  \"}" })
    void extractErrorMessage_shouldFallbackToStatus_whenJsonCarriesNoTextualMessage(final String body) {
        assertThat(ErrorMessageUtil.extractErrorMessage(errorResponse(422, body))).isEqualTo("HTTP 422");
    }

    @Test
    void extractErrorMessage_shouldTakeFieldsInPrecedenceOrder_skippingBlankOnes() {
        assertThat(ErrorMessageUtil
                .extractErrorMessage(errorResponse(422, "{\"title\":\"t\",\"error\":\"e\",\"message\":\" \"}")))
                .isEqualTo("e");
    }

    @Test
    void extractErrorMessage_shouldCapLongMessages_andRemoveControlCharacters() {
        final var result = ErrorMessageUtil
                .extractErrorMessage(errorResponse(422, "{\"message\":\"line1\\r\\nforged " + "x".repeat(300) + "\"}"));

        assertThat(result).startsWith("line1  forged ").doesNotContain("\r", "\n")
                .hasSize(ErrorMessageUtil.MAX_MESSAGE_LENGTH + 1).endsWith("…");
    }
}
