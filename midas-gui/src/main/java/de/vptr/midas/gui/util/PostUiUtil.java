package de.vptr.midas.gui.util;

import java.time.format.DateTimeFormatter;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.vptr.midas.gui.dto.PostResponseDto;

/** Helper utility for post UI rendering to prevent code duplication. */
public final class PostUiUtil {

    private PostUiUtil() {
        // Utility class
    }

    /** Creates a metadata layout row for a post. */
    public static HorizontalLayout createPostMetadataLayout(final PostResponseDto post,
            final DateTimeFormatter formatter, final boolean includeEditTime, final boolean includeCommentsCount,
            final String publishedPrefix) {
        final var metaLayout = new HorizontalLayout();
        metaLayout.setSpacing(true);
        metaLayout.getStyle().set("font-size", "var(--lumo-font-size-s)");
        metaLayout.getStyle().set("color", "var(--lumo-secondary-text-color)");

        if (post.getUsername() != null) {
            metaLayout.add(new Span("By " + post.getUsername()));
        }

        if (post.getCreated() != null) {
            metaLayout.add(new Span(publishedPrefix + post.getCreated().format(formatter)));
        }

        if (includeEditTime && post.getLastEdit() != null && !post.getLastEdit().equals(post.getCreated())) {
            metaLayout.add(new Span("(edited " + post.getLastEdit().format(formatter) + ")"));
        }

        if (post.getCategoryName() != null) {
            final var categoryBadge = new Span(post.getCategoryName());
            categoryBadge.getStyle().set("background", "var(--lumo-primary-color-10pct)");
            categoryBadge.getStyle().set("border-radius", "var(--lumo-border-radius-s)");
            categoryBadge.getStyle().set("padding", "2px 8px");
            metaLayout.add(categoryBadge);
        }

        if (includeCommentsCount && post.getCommentsCount() != null && post.getCommentsCount() > 0) {
            metaLayout.add(new Span(post.getCommentsCount() + " comments"));
        }

        return metaLayout;
    }
}
