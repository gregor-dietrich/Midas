package de.vptr.midas.gui.component.layout;

import java.util.function.Consumer;
import java.util.function.LongSupplier;

import jakarta.annotation.Nullable;

final class SearchExecutionSupport {

    static final int MAX_QUERY_LENGTH = 255;
    private static final long DEFAULT_THROTTLE_MILLIS = 400L;

    private final LongSupplier currentTimeMillisSupplier;
    private final long throttleMillis;
    private String lastSubmittedQuery = "";
    private long lastSubmittedAtMillis = Long.MIN_VALUE;

    SearchExecutionSupport() {
        this(System::currentTimeMillis, DEFAULT_THROTTLE_MILLIS);
    }

    SearchExecutionSupport(final LongSupplier currentTimeMillisSupplier, final long throttleMillis) {
        this.currentTimeMillisSupplier = currentTimeMillisSupplier;
        this.throttleMillis = throttleMillis;
    }

    @Nullable
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
        final boolean throttled =
                query.equals(this.lastSubmittedQuery) && now - this.lastSubmittedAtMillis < this.throttleMillis;
        if (throttled) {
            return false;
        }

        this.lastSubmittedQuery = query;
        this.lastSubmittedAtMillis = now;
        return true;
    }

    /**
     * Normalizes the raw input, optionally updates the field via {@code fieldUpdater} if the value was trimmed, and
     * invokes {@code action} if execution is not throttled.
     *
     * @param rawValue
     *            the current raw value from the text field
     * @param fieldUpdater
     *            called with the normalized value when the field text needs to be corrected
     * @param action
     *            called with the normalized query when execution should proceed
     * @return {@code true} if the action was invoked
     */
    boolean execute(final String rawValue, final Consumer<String> fieldUpdater, final Consumer<String> action) {
        final String query = this.normalize(rawValue);
        if (query == null) {
            return false;
        }
        if (!query.equals(rawValue)) {
            fieldUpdater.accept(query);
        }
        if (this.shouldExecute(query)) {
            action.accept(query);
            return true;
        }
        return false;
    }
}
