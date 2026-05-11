package de.vptr.midas.gui.component.layout;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.LinguisticNaming")
class AsyncSearchLayoutTest {

    @Test
    void constructor_shouldCreateComponentsWithDefaults() {
        final var layout = new AsyncSearchLayout(e -> {
        }, () -> {
        });

        assertThat(layout.getTextfield()).isNotNull();
        assertThat(layout.getSearchButton()).isNotNull();
        assertThat(layout.getClearButton()).isNotNull();
    }

    @Test
    void constructor_shouldApplyCustomPlaceholderAndTooltip() {
        final var layout = new AsyncSearchLayout(e -> {
        }, () -> {
        }, "Find users", "Search users");

        assertThat(layout.getTextfield().getPlaceholder()).isEqualTo("Find users");
    }

    @Test
    void setSearching_shouldDisableAllComponents_whenTrue() {
        final var layout = new AsyncSearchLayout(e -> {
        }, () -> {
        });

        layout.setSearching(true);

        assertThat(layout.getSearchButton().isEnabled()).isFalse();
        assertThat(layout.getTextfield().isEnabled()).isFalse();
        assertThat(layout.getClearButton().isEnabled()).isFalse();
    }

    @Test
    void setSearching_shouldEnableAllComponents_whenFalse() {
        final var layout = new AsyncSearchLayout(e -> {
        }, () -> {
        });
        layout.setSearching(true);

        layout.setSearching(false);

        assertThat(layout.getSearchButton().isEnabled()).isTrue();
        assertThat(layout.getTextfield().isEnabled()).isTrue();
        assertThat(layout.getClearButton().isEnabled()).isTrue();
    }

    @Test
    void getValue_shouldReturnTextFieldValue() {
        final var layout = new AsyncSearchLayout(e -> {
        }, () -> {
        });
        layout.getTextfield().setValue("test query");

        assertThat(layout.getValue()).isEqualTo("test query");
    }

    @Test
    void getValue_shouldReturnEmptyString_whenNoInput() {
        final var layout = new AsyncSearchLayout(e -> {
        }, () -> {
        });

        assertThat(layout.getValue()).isEmpty();
    }

    @Test
    void clearButton_shouldClearTextField() {
        final var cleared = new AtomicBoolean(false);
        final var layout = new AsyncSearchLayout(e -> {
        }, () -> cleared.set(true));

        layout.getTextfield().setValue("something");
        layout.getClearButton().click();

        assertThat(layout.getTextfield().getValue()).isEmpty();
        assertThat(cleared.get()).isTrue();
    }

    @Test
    void constructor_shouldApplyCustomWidth() {
        final var layout = new AsyncSearchLayout(e -> {
        }, () -> {
        }, "Search...", "Tooltip", "500px");

        assertThat(layout.getTextfield().getWidth()).isEqualTo("500px");
    }

    @Test
    void searchButton_shouldTrimQueryBeforeExecutingSearch() {
        final var executedSearches = new AtomicInteger();
        final var layout = new AsyncSearchLayout(query -> {
            assertThat(query).isEqualTo("users");
            executedSearches.incrementAndGet();
        }, () -> {
        }, "Search...", "Search", "300px", new SearchExecutionSupport());

        layout.getTextfield().setValue("  users  ");
        layout.getSearchButton().click();

        assertThat(layout.getTextfield().getValue()).isEqualTo("users");
        assertThat(executedSearches.get()).isEqualTo(1);
    }

    @Test
    void searchButton_shouldIgnoreBlankQuery() {
        final var executedSearches = new AtomicInteger();
        final var layout = new AsyncSearchLayout(e -> executedSearches.incrementAndGet(), () -> {
        }, "Search...", "Search", "300px", new SearchExecutionSupport());

        layout.getTextfield().setValue("   ");
        layout.getSearchButton().click();

        assertThat(executedSearches.get()).isZero();
    }

    @Test
    void searchButton_shouldThrottleRepeatedQuery() {
        final var executedSearches = new AtomicInteger();
        final var currentTimeMillis = new AtomicLong(1_000L);
        final var layout = new AsyncSearchLayout(e -> executedSearches.incrementAndGet(), () -> {
        }, "Search...", "Search", "300px", new SearchExecutionSupport(currentTimeMillis::get, 400L));

        layout.getTextfield().setValue("users");
        layout.getSearchButton().click();
        currentTimeMillis.addAndGet(200L);
        layout.getSearchButton().click();
        currentTimeMillis.addAndGet(400L);
        layout.getSearchButton().click();

        assertThat(executedSearches.get()).isEqualTo(2);
    }
}
