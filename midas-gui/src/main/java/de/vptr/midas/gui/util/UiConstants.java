package de.vptr.midas.gui.util;

/**
 * Named constants for timeouts, retries, limits, and buffer sizes scattered across the GUI module.
 */
public final class UiConstants {

    private UiConstants() {
        // utility class
    }

    /** Max retries for REST client calls via MicroProfile Fault Tolerance. */
    public static final int MAX_RETRIES = 2;

    /** Delay between retries in milliseconds. */
    public static final long RETRY_DELAY_MS = 500;

    /** Per-request timeout for REST client calls in milliseconds. */
    public static final long REQUEST_TIMEOUT_MS = 2000;

    /** Session TTL in minutes. */
    public static final int SESSION_TTL_MINUTES = 30;

    /** Default grid page size for admin lazy data providers. */
    public static final int DEFAULT_GRID_PAGE_SIZE = 50;

    /** Default page size for general list queries. */
    public static final int DEFAULT_PAGE_SIZE = 50;

    /** Large page size for dropdown / combobox data loading. */
    public static final int LARGE_PAGE_SIZE = 200;

    /** Max payments returned per account query. */
    public static final int MAX_PAYMENTS_PER_QUERY = 1_000;
}
