package de.vptr.midas.gui.util;

import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.Nullable;
import jakarta.ws.rs.core.Response;

/**
 * Utility class for extracting error messages from HTTP responses.
 */
public final class ErrorMessageUtil {

    private static final Logger LOG = Logger.getLogger(ErrorMessageUtil.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    /** Longest backend message shown to the user or logged. */
    static final int MAX_MESSAGE_LENGTH = 200;
    private static final Pattern CONTROL_CHARACTERS = Pattern.compile("\\p{Cntrl}");

    private ErrorMessageUtil() {
        // Utility class
    }

    /**
     * Returns the first non-blank textual {@code message}, {@code detail}, {@code error} or {@code title} of a JSON
     * error object in the response body, capped at {@value #MAX_MESSAGE_LENGTH} characters with control characters
     * removed. Any other body (plain text, HTML, JSON without such a field) falls back to {@code HTTP <status>}, so raw
     * backend or proxy text never reaches the user or the log.
     */
    public static String extractErrorMessage(final Response response) {
        if (response == null) {
            return "HTTP unknown";
        }

        try {
            if (response.hasEntity() && response.getStatus() >= 400) {
                response.bufferEntity();
                final String responseBody = response.readEntity(String.class);
                final String parsedMessage =
                        responseBody == null || responseBody.isBlank() ? null : parseMessageFromBody(responseBody);
                if (parsedMessage != null) {
                    return parsedMessage;
                }
            }
        } catch (final RuntimeException e) {
            LOG.warnf("Failed to extract error message from %s response: %s", response.getStatus(), e.getMessage());
        }

        return "HTTP " + response.getStatus();
    }

    @Nullable
    private static String parseMessageFromBody(final String body) {
        try {
            final JsonNode root = OBJECT_MAPPER.readTree(body);
            return root != null && root.isObject() ? firstNonBlankText(root, "message", "detail", "error", "title")
                    : null;
        } catch (final JsonProcessingException e) {
            // Not valid JSON: never surface a raw body (proxy pages, stack traces), fall back to the status
            return null;
        }
    }

    @Nullable
    private static String firstNonBlankText(final JsonNode root, final String... fieldNames) {
        for (final String fieldName : fieldNames) {
            final JsonNode field = root.get(fieldName);
            if (field != null && field.isTextual()) {
                final String value = CONTROL_CHARACTERS.matcher(field.asText()).replaceAll(" ").trim();
                if (!value.isEmpty()) {
                    return value.length() > MAX_MESSAGE_LENGTH ? value.substring(0, MAX_MESSAGE_LENGTH) + "…" : value;
                }
            }
        }
        return null;
    }
}
