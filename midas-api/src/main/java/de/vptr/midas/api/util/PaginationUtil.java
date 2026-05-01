package de.vptr.midas.api.util;

/** Shared pagination normalization helpers for REST resources. */
public final class PaginationUtil {
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int DEFAULT_RECENT_LIMIT = 10;
    public static final int MAX_SIZE = 100;

    private PaginationUtil() {
    }

    /**
     * Normalizes page number to the default page
     * when the provided value is negative.
     */
    public static int sanitizePage(final int page) {
        return Math.max(DEFAULT_PAGE, page);
    }

    /** Normalizes page size to defaults and enforced max. */
    public static int sanitizeSize(final int size) {
        if (size <= 0) {
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
