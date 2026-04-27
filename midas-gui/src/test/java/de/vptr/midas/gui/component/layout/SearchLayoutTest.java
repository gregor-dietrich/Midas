package de.vptr.midas.gui.component.layout;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class SearchLayoutTest {

    @Test
    void searchButton_shouldTrimQueryBeforeExecutingSearch() {
        final var executedSearches = new AtomicInteger();
        final var layout = new SearchLayout(
                _ -> {
                },
                _ -> executedSearches.incrementAndGet(),
                "Search...",
                "Search",
                "Search",
                "300px",
                new SearchExecutionSupport());

        layout.getTextfield().setValue("  users  ");
        layout.getButton().click();

        assertThat(layout.getTextfield().getValue()).isEqualTo("users");
        assertThat(executedSearches.get()).isEqualTo(1);
    }

    @Test
    void searchButton_shouldIgnoreBlankQuery() {
        final var executedSearches = new AtomicInteger();
        final var layout = new SearchLayout(
                _ -> {
                },
                _ -> executedSearches.incrementAndGet(),
                "Search...",
                "Search",
                "Search",
                "300px",
                new SearchExecutionSupport());

        layout.getTextfield().setValue("   ");
        layout.getButton().click();

        assertThat(executedSearches.get()).isZero();
    }

    @Test
    void searchButton_shouldThrottleRepeatedQuery() {
        final var executedSearches = new AtomicInteger();
        final var currentTimeMillis = new AtomicLong(1_000L);
        final var layout = new SearchLayout(
                _ -> {
                },
                _ -> executedSearches.incrementAndGet(),
                "Search...",
                "Search",
                "Search",
                "300px",
                new SearchExecutionSupport(currentTimeMillis::get, 400L));

        layout.getTextfield().setValue("users");
        layout.getButton().click();
        currentTimeMillis.addAndGet(200L);
        layout.getButton().click();
        currentTimeMillis.addAndGet(400L);
        layout.getButton().click();

        assertThat(executedSearches.get()).isEqualTo(2);
    }
}
