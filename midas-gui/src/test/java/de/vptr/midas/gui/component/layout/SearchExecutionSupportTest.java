package de.vptr.midas.gui.component.layout;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class SearchExecutionSupportTest {

    @Test
    void normalize_shouldTrimAndTruncateOverlongQueries() {
        final var searchExecutionSupport = new SearchExecutionSupport();
        final var originalQuery = " " + "x".repeat(SearchExecutionSupport.MAX_QUERY_LENGTH + 5) + " ";

        final var normalizedQuery = searchExecutionSupport.normalize(originalQuery);

        assertThat(normalizedQuery).isEqualTo("x".repeat(SearchExecutionSupport.MAX_QUERY_LENGTH));
    }

    @Test
    void shouldExecute_shouldAllowDifferentQueriesWithinThrottleWindow() {
        final var currentTimeMillis = new AtomicLong(1_000L);
        final var searchExecutionSupport = new SearchExecutionSupport(currentTimeMillis::get, 400L);

        assertThat(searchExecutionSupport.shouldExecute("users")).isTrue();
        currentTimeMillis.addAndGet(200L);

        assertThat(searchExecutionSupport.shouldExecute("pages")).isTrue();
    }
}
