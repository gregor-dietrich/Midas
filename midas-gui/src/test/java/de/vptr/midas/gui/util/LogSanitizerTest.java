package de.vptr.midas.gui.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LogSanitizerTest {

    @Test
    void sanitizeReplacesCarriageReturnsAndLineFeeds() {
        assertEquals("a\\rb\\nc\\r\\nd", LogSanitizer.sanitize("a\rb\nc\r\nd"));
    }

    @Test
    void sanitizeHandlesNullAndPlainValues() {
        assertEquals("null", LogSanitizer.sanitize(null));
        assertEquals("plain-value", LogSanitizer.sanitize("plain-value"));
        assertEquals("42", LogSanitizer.sanitize(42));
    }
}
