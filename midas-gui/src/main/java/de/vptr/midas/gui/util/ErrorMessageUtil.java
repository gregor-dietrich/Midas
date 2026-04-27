package de.vptr.midas.gui.util;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.core.Response;

/**
 * Utility class for extracting error messages from HTTP responses.
 */
public class ErrorMessageUtil {

    private static final Logger LOG = Logger.getLogger(ErrorMessageUtil.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Extracts error message from HTTP response body.
     * Attempts to parse structured error response, falls back to status text.
     */
    public static String extractErrorMessage(final Response response) {
        if (response == null) {
            return "HTTP unknown";
        }

        try {
            if (response.hasEntity() && response.getStatus() >= 400) {
                response.bufferEntity();
                final String responseBody = response.readEntity(String.class);
                if (responseBody != null && !responseBody.trim().isEmpty()) {
                    final String parsedMessage = parseMessageFromBody(responseBody);
                    if (parsedMessage != null && !parsedMessage.isBlank()) {
                        return parsedMessage;
                    }
                }
            }
        } catch (final RuntimeException e) {
            LOG.warnf("Failed to extract error message from %s response: %s", response.getStatus(), e.getMessage());
        }

        // Fall back to HTTP status
        return "HTTP " + response.getStatus();
    }

    private static String parseMessageFromBody(final String body) {
        try {
            final JsonNode root = OBJECT_MAPPER.readTree(body);
            if (root != null && root.isObject()) {
                final String structuredMessage = firstNonBlankText(root, "message", "detail", "error", "title");
                if (structuredMessage != null) {
                    return structuredMessage;
                }
            }
        } catch (final JsonProcessingException e) {
            // Not valid JSON — fall through to raw body
        }

        final String trimmed = body.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("<")) {
            return null;
        }
        return trimmed;
    }

    private static String firstNonBlankText(final JsonNode root, final String... fieldNames) {
        for (final String fieldName : fieldNames) {
            final JsonNode field = root.get(fieldName);
            if (field != null && field.isTextual()) {
                final String value = field.asText().trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }
}
