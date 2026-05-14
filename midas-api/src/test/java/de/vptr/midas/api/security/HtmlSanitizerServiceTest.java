package de.vptr.midas.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
@SuppressWarnings("NullAway")
class HtmlSanitizerServiceTest {

    @Inject
    HtmlSanitizerService sanitizer;

    @Test
    void sanitizeRichContent_shouldReturnNullForNull() {
        assertNull(this.sanitizer.sanitizeRichContent(null));
    }

    @Test
    void sanitizeRichContent_shouldPassThroughPlainText() {
        assertEquals("Hello World", this.sanitizer.sanitizeRichContent("Hello World"));
    }

    @Test
    void sanitizeRichContent_shouldStripScriptTags() {
        final String result = this.sanitizer.sanitizeRichContent("<script>alert(1)</script>");
        assertFalse(result.contains("<script>"), "script tag must be removed");
        assertFalse(result.contains("alert("), "script body must be removed");
    }

    @Test
    void sanitizeRichContent_shouldPreserveSafeFormatting() {
        assertEquals("<b>bold</b>", this.sanitizer.sanitizeRichContent("<b>bold</b>"));
    }

    @Test
    void sanitizeComment_shouldReturnNullForNull() {
        assertNull(this.sanitizer.sanitizeComment(null));
    }

    @Test
    void sanitizeComment_shouldStripScriptTags() {
        final String result = this.sanitizer.sanitizeComment("<script>evil()</script>");
        assertFalse(result.contains("<script>"), "script tag must be removed");
    }

    @Test
    void sanitizeComment_shouldStripLinksAndPreserveText() {
        final String result = this.sanitizer.sanitizeComment("<a href=\"http://evil.com\">click here</a>");
        assertFalse(result.contains("<a"), "anchor tag must be stripped in comments");
        assertEquals("click here", result);
    }
}
