package de.vptr.midas.gui.util;

/** Shared pagination parameter normalization for GUI REST clients. */
public final class PaginationParamUtil {
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int DEFAULT_RECENT_LIMIT = 10;
    public static final int MAX_SIZE = 100;

    private PaginationParamUtil() {
    }

    /** Normalizes page index to a non-negative value. */
    public static Integer sanitizePage(final Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }
        return Math.max(DEFAULT_PAGE, page);
    }

    /** Normalizes page size to defaults and enforced max. */
    public static Integer sanitizeSize(final Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    /** Normalizes recent-item limit to defaults and enforced max. */
    public static int sanitizeRecentLimit(final int limit) {
        if (limit <= 0) {
            return DEFAULT_RECENT_LIMIT;
        }
        return Math.min(limit, MAX_SIZE);
    }
}
