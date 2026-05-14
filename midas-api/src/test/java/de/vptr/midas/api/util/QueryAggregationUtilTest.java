package de.vptr.midas.api.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class QueryAggregationUtilTest {

    @Test
    void groupLongValuesByKey_shouldReturnEmptyMap_whenEmptyList() {
        assertTrue(QueryAggregationUtil.groupLongValuesByKey(List.of()).isEmpty());
    }

    @Test
    void groupLongValuesByKey_shouldGroupValuesCorrectly() {
        final var rows = List.of(new Object[] { 1L, 10L }, new Object[] { 1L, 20L }, new Object[] { 2L, 30L });

        final var result = QueryAggregationUtil.groupLongValuesByKey(rows);

        assertEquals(2, result.size());
        assertEquals(List.of(10L, 20L), result.get(1L));
        assertEquals(List.of(30L), result.get(2L));
    }

    @Test
    void groupStringValuesByKey_shouldReturnEmptyMap_whenEmptyList() {
        assertTrue(QueryAggregationUtil.groupStringValuesByKey(List.of()).isEmpty());
    }

    @Test
    void groupStringValuesByKey_shouldGroupValuesCorrectly() {
        final var rows =
                List.of(new Object[] { 1L, "alice" }, new Object[] { 1L, "bob" }, new Object[] { 2L, "carol" });

        final var result = QueryAggregationUtil.groupStringValuesByKey(rows);

        assertEquals(2, result.size());
        assertEquals(List.of("alice", "bob"), result.get(1L));
        assertEquals(List.of("carol"), result.get(2L));
    }

    @Test
    void toLongCountMap_shouldReturnEmptyMap_whenEmptyList() {
        assertTrue(QueryAggregationUtil.toLongCountMap(List.of()).isEmpty());
    }

    @Test
    void toLongCountMap_shouldMapCorrectly() {
        final var rows = List.of(new Object[] { 1L, 5L }, new Object[] { 2L, 3L });

        final var result = QueryAggregationUtil.toLongCountMap(rows);

        assertEquals(2, result.size());
        assertEquals(5L, result.get(1L));
        assertEquals(3L, result.get(2L));
    }
}
