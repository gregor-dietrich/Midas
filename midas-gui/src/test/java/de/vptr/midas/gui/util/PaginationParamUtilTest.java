package de.vptr.midas.gui.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PaginationParamUtilTest {

    @Test
    void sanitizePage_shouldClampNegativeAndNullToZero() {
        assertThat(PaginationParamUtil.sanitizePage(null)).isEqualTo(0);
        assertThat(PaginationParamUtil.sanitizePage(-4)).isEqualTo(0);
        assertThat(PaginationParamUtil.sanitizePage(3)).isEqualTo(3);
    }

    @Test
    void sanitizeSize_shouldApplyDefaultAndCap() {
        assertThat(PaginationParamUtil.sanitizeSize(null)).isEqualTo(20);
        assertThat(PaginationParamUtil.sanitizeSize(0)).isEqualTo(20);
        assertThat(PaginationParamUtil.sanitizeSize(-2)).isEqualTo(20);
        assertThat(PaginationParamUtil.sanitizeSize(200)).isEqualTo(100);
        assertThat(PaginationParamUtil.sanitizeSize(25)).isEqualTo(25);
    }

    @Test
    void sanitizeRecentLimit_shouldApplyDefaultAndCap() {
        assertThat(PaginationParamUtil.sanitizeRecentLimit(0)).isEqualTo(10);
        assertThat(PaginationParamUtil.sanitizeRecentLimit(-8)).isEqualTo(10);
        assertThat(PaginationParamUtil.sanitizeRecentLimit(150)).isEqualTo(100);
        assertThat(PaginationParamUtil.sanitizeRecentLimit(30)).isEqualTo(30);
    }
}
