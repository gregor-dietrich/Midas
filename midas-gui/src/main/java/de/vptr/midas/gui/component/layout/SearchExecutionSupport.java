package de.vptr.midas.gui.component.layout;

import java.util.function.LongSupplier;

final class SearchExecutionSupport {

    static final int MAX_QUERY_LENGTH = 255;
    private static final long DEFAULT_THROTTLE_MILLIS = 400L;

    private final LongSupplier currentTimeMillisSupplier;
    private final long throttleMillis;
    private String lastSubmittedQuery;
    private long lastSubmittedAtMillis = Long.MIN_VALUE;

    SearchExecutionSupport() {
        this(System::currentTimeMillis, DEFAULT_THROTTLE_MILLIS);
    }

    SearchExecutionSupport(final LongSupplier currentTimeMillisSupplier, final long throttleMillis) {
        this.currentTimeMillisSupplier = currentTimeMillisSupplier;
        this.throttleMillis = throttleMillis;
    }

    String normalize(final String value) {
        if (value == null) {
            return null;
        }

        final String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            return null;
        }

        if (trimmedValue.length() <= MAX_QUERY_LENGTH) {
            return trimmedValue;
        }

        return trimmedValue.substring(0, MAX_QUERY_LENGTH);
    }

    boolean shouldExecute(final String query) {
        if (query == null) {
            return false;
        }

        final long now = this.currentTimeMillisSupplier.getAsLong();
        final boolean throttled = query.equals(this.lastSubmittedQuery)
                && now - this.lastSubmittedAtMillis < this.throttleMillis;
        if (throttled) {
            return false;
        }

        this.lastSubmittedQuery = query;
        this.lastSubmittedAtMillis = now;
        return true;
    }
}
