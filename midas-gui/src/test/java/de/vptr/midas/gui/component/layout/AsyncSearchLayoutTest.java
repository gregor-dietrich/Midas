package de.vptr.midas.gui.component.layout;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class AsyncSearchLayoutTest {

    @Test
    void constructor_shouldCreateComponentsWithDefaults() {
        final var layout = new AsyncSearchLayout(_ -> {
        }, () -> {
        });

        assertThat(layout.getTextField()).isNotNull();
        assertThat(layout.getSearchButton()).isNotNull();
        assertThat(layout.getClearButton()).isNotNull();
    }

    @Test
    void constructor_shouldApplyCustomPlaceholderAndTooltip() {
        final var layout = new AsyncSearchLayout(_ -> {
        }, () -> {
        }, "Find users", "Search users");

        assertThat(layout.getTextField().getPlaceholder()).isEqualTo("Find users");
    }

    @Test
    void setSearching_shouldDisableAllComponents_whenTrue() {
        final var layout = new AsyncSearchLayout(_ -> {
        }, () -> {
        });

        layout.setSearching(true);

        assertThat(layout.getSearchButton().isEnabled()).isFalse();
        assertThat(layout.getTextField().isEnabled()).isFalse();
        assertThat(layout.getClearButton().isEnabled()).isFalse();
    }

    @Test
    void setSearching_shouldEnableAllComponents_whenFalse() {
        final var layout = new AsyncSearchLayout(_ -> {
        }, () -> {
        });
        layout.setSearching(true);

        layout.setSearching(false);

        assertThat(layout.getSearchButton().isEnabled()).isTrue();
        assertThat(layout.getTextField().isEnabled()).isTrue();
        assertThat(layout.getClearButton().isEnabled()).isTrue();
    }

    @Test
    void getValue_shouldReturnTextFieldValue() {
        final var layout = new AsyncSearchLayout(_ -> {
        }, () -> {
        });
        layout.getTextField().setValue("test query");

        assertThat(layout.getValue()).isEqualTo("test query");
    }

    @Test
    void getValue_shouldReturnEmptyString_whenNoInput() {
        final var layout = new AsyncSearchLayout(_ -> {
        }, () -> {
        });

        assertThat(layout.getValue()).isEmpty();
    }

    @Test
    void clearButton_shouldClearTextField() {
        final var cleared = new AtomicBoolean(false);
        final var layout = new AsyncSearchLayout(_ -> {
        }, () -> cleared.set(true));

        layout.getTextField().setValue("something");
        layout.getClearButton().click();

        assertThat(layout.getTextField().getValue()).isEmpty();
        assertThat(cleared.get()).isTrue();
    }

    @Test
    void constructor_shouldApplyCustomWidth() {
        final var layout = new AsyncSearchLayout(_ -> {
        }, () -> {
        }, "Search...", "Tooltip", "500px");

        assertThat(layout.getTextField().getWidth()).isEqualTo("500px");
    }

    @Test
    void searchButton_shouldTrimQueryBeforeExecutingSearch() {
        final var executedSearches = new AtomicInteger();
        final var layout = new AsyncSearchLayout(
                query -> {
                    assertThat(query).isEqualTo("users");
                    executedSearches.incrementAndGet();
                },
                () -> {
                },
                "Search...",
                "Search",
                "300px",
                new SearchExecutionSupport());

        layout.getTextField().setValue("  users  ");
        layout.getSearchButton().click();

        assertThat(layout.getTextField().getValue()).isEqualTo("users");
        assertThat(executedSearches.get()).isEqualTo(1);
    }

    @Test
    void searchButton_shouldIgnoreBlankQuery() {
        final var executedSearches = new AtomicInteger();
        final var layout = new AsyncSearchLayout(
                _ -> executedSearches.incrementAndGet(),
                () -> {
                },
                "Search...",
                "Search",
                "300px",
                new SearchExecutionSupport());

        layout.getTextField().setValue("   ");
        layout.getSearchButton().click();

        assertThat(executedSearches.get()).isZero();
    }

    @Test
    void searchButton_shouldThrottleRepeatedQuery() {
        final var executedSearches = new AtomicInteger();
        final var currentTimeMillis = new AtomicLong(1_000L);
        final var layout = new AsyncSearchLayout(
                _ -> executedSearches.incrementAndGet(),
                () -> {
                },
                "Search...",
                "Search",
                "300px",
                new SearchExecutionSupport(currentTimeMillis::get, 400L));

        layout.getTextField().setValue("users");
        layout.getSearchButton().click();
        currentTimeMillis.addAndGet(200L);
        layout.getSearchButton().click();
        currentTimeMillis.addAndGet(400L);
        layout.getSearchButton().click();

        assertThat(executedSearches.get()).isEqualTo(2);
    }
}
