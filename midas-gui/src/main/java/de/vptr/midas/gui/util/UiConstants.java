package de.vptr.midas.gui.util;

/**
 * Named constants for timeouts, retries, limits, and buffer sizes scattered across the GUI module.
 */
public final class UiConstants {

    private UiConstants() {
        // utility class
    }

    /** Retries for an idempotent request after a connection failure or timeout (MicroProfile Fault Tolerance). */
    public static final int MAX_RETRIES = 2;

    /** Retries for a GET: safe and idempotent, so a connection failure or timeout may be retried. */
    public static final int RETRIES_GET = MAX_RETRIES;

    /** Retries for a PUT: idempotent, so sending it again after a lost response does no harm. */
    public static final int RETRIES_PUT = MAX_RETRIES;

    /** Retries for a DELETE: idempotent, so sending it again after a lost response does no harm. */
    public static final int RETRIES_DELETE = MAX_RETRIES;

    /** Retries for a POST: none, since a timed-out POST may have been applied and a retry would apply it twice. */
    public static final int RETRIES_POST = 0;

    /** Retries for a PATCH: none, since a PATCH is not guaranteed to be idempotent. */
    public static final int RETRIES_PATCH = 0;

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
