package de.vptr.midas.api.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shared helpers for small aggregate query result maps. */
public final class QueryAggregationUtil {

    private QueryAggregationUtil() {
    }

    /** Groups the second column by the first column. */
    public static Map<Long, List<Long>> groupLongValuesByKey(final List<Object[]> rows) {
        final Map<Long, List<Long>> valuesByKey = new HashMap<>();
        for (final Object[] row : rows) {
            valuesByKey.computeIfAbsent((Long) row[0], ignored -> new ArrayList<>()).add((Long) row[1]);
        }
        return valuesByKey;
    }

    /** Maps id/count rows into a lookup map. */
    public static Map<Long, Long> toLongCountMap(final List<Object[]> rows) {
        final Map<Long, Long> countsById = new HashMap<>();
        for (final Object[] row : rows) {
            countsById.put((Long) row[0], (Long) row[1]);
        }
        return countsById;
    }
}
