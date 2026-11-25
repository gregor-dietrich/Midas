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

        final var username = post.getUsername();
        if (username != null) {
            metaLayout.add(new Span("By " + username));
        }

        final var created = post.getCreated();
        if (created != null) {
            metaLayout.add(new Span(publishedPrefix + created.format(formatter)));
        }

        if (includeEditTime) {
            addEditTimeIfPresent(metaLayout, post, formatter);
        }

        addCategoryBadgeIfPresent(metaLayout, post);

        final var commentsCount = post.getCommentsCount();
        if (includeCommentsCount && commentsCount != null && commentsCount > 0) {
            metaLayout.add(new Span(commentsCount + " comments"));
        }

        return metaLayout;
    }

    private static void addEditTimeIfPresent(final HorizontalLayout metaLayout, final PostResponseDto post,
            final DateTimeFormatter formatter) {
        final var lastEdit = post.getLastEdit();
        if (lastEdit != null && !lastEdit.equals(post.getCreated())) {
            metaLayout.add(new Span("(edited " + lastEdit.format(formatter) + ")"));
        }
    }

    private static void addCategoryBadgeIfPresent(final HorizontalLayout metaLayout, final PostResponseDto post) {
        final var categoryName = post.getCategoryName();
        if (categoryName != null) {
            final var categoryBadge = new Span(categoryName);
            categoryBadge.getStyle().set("background", "var(--lumo-primary-color-10pct)");
            categoryBadge.getStyle().set("border-radius", "var(--lumo-border-radius-s)");
            categoryBadge.getStyle().set("padding", "2px 8px");
            metaLayout.add(categoryBadge);
        }
    }
}
