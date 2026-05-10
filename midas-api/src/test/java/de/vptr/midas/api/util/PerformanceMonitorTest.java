package de.vptr.midas.api.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PerformanceMonitorTest {

    @Test
    void testTime_supplier_returnsValue() {
        final String result = PerformanceMonitor.time("test-op", () -> "hello");

        assertEquals("hello", result);
    }

    @Test
    void testTime_supplier_propagatesException() {
        final RuntimeException expected = new RuntimeException("supplier failed");

        final RuntimeException thrown =
                assertThrows(RuntimeException.class, () -> PerformanceMonitor.time("failing-op", () -> {
                    throw expected;
                }));

        assertSame(expected, thrown);
    }

    @Test
    void testTime_runnable_executesRunnable() {
        final boolean[] executed = { false };

        PerformanceMonitor.time("runnable-op", () -> executed[0] = true);

        assertTrue(executed[0]);
    }

    @Test
    void testTime_runnable_propagatesException() {
        final RuntimeException expected = new RuntimeException("runnable failed");

        final RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> PerformanceMonitor.time("failing-runnable", (Runnable) () -> {
                    throw expected;
                }));

        assertSame(expected, thrown);
    }
}
