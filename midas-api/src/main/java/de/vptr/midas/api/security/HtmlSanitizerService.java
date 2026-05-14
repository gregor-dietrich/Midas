package de.vptr.midas.api.security;

import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;

/** Sanitizes user-supplied HTML content to prevent XSS. */
@ApplicationScoped
public class HtmlSanitizerService {

    private static final PolicyFactory RICH_CONTENT_POLICY = Sanitizers.FORMATTING.and(Sanitizers.LINKS)
            .and(Sanitizers.BLOCKS).and(Sanitizers.IMAGES).and(Sanitizers.TABLES);

    private static final PolicyFactory COMMENT_POLICY = Sanitizers.FORMATTING.and(Sanitizers.BLOCKS);

    /** Sanitizes rich HTML content (posts, pages) — allows formatting, links, images, and tables. */
    @Nullable
    public String sanitizeRichContent(@Nullable final String html) {
        if (html == null) {
            return null;
        }
        return RICH_CONTENT_POLICY.sanitize(html);
    }

    /** Sanitizes comment content — allows only inline formatting and block elements, no links or images. */
    @Nullable
    public String sanitizeComment(@Nullable final String html) {
        if (html == null) {
            return null;
        }
        return COMMENT_POLICY.sanitize(html);
    }
}
