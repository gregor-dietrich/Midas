package de.vptr.midas.api.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PaginationUtilTest {

    @Test
    void sanitizePage_shouldClampNegativeToZero() {
        assertEquals(0, PaginationUtil.sanitizePage(-3));
    }

    @Test
    void sanitizeSize_shouldUseDefaultsAndCap() {
        assertEquals(20, PaginationUtil.sanitizeSize(0));
        assertEquals(20, PaginationUtil.sanitizeSize(-5));
        assertEquals(100, PaginationUtil.sanitizeSize(500));
        assertEquals(50, PaginationUtil.sanitizeSize(50));
    }

    @Test
    void sanitizeRecentLimit_shouldUseDefaultsAndCap() {
        assertEquals(10, PaginationUtil.sanitizeRecentLimit(0));
        assertEquals(10, PaginationUtil.sanitizeRecentLimit(-1));
        assertEquals(100, PaginationUtil.sanitizeRecentLimit(1000));
        assertEquals(25, PaginationUtil.sanitizeRecentLimit(25));
    }
}
