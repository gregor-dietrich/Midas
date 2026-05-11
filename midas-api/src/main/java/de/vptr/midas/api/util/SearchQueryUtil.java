package de.vptr.midas.api.util;

import java.util.Locale;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import jakarta.annotation.Nullable;

/** Shared search query normalization helpers. */
public final class SearchQueryUtil {
    private static final String LIKE_ESCAPE = "\\";
    private static final String ESCAPED_LIKE_ESCAPE = "\\\\";

    private SearchQueryUtil() {
    }

    /**
     * Returns a normalized contains-pattern for LIKE queries, or null for blank input.
     */
    @Nullable
    public static String toContainsLikePattern(@Nullable final String query) {
        if (query == null) {
            return null;
        }
        final String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        if (normalizedQuery.isEmpty()) {
            return null;
        }
        return "%" + escapeLike(normalizedQuery) + "%";
    }

    /** Applies shared first-page search limits. */
    public static <T> PanacheQuery<T> applyDefaultSearchLimit(final PanacheQuery<T> query) {
        return query.page(PaginationUtil.DEFAULT_PAGE, PaginationUtil.MAX_SIZE);
    }

    private static String escapeLike(final String query) {
        return query.replace(LIKE_ESCAPE, ESCAPED_LIKE_ESCAPE).replace("%", LIKE_ESCAPE + "%").replace("_",
                LIKE_ESCAPE + "_");
    }
}
