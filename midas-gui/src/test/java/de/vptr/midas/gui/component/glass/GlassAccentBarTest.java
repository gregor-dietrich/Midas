package de.vptr.midas.gui.component.glass;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GlassAccentBarTest {

    @Test
    void shouldHaveExpectedStyles() {
        final var bar = new GlassAccentBar();

        assertThat(bar.getStyle().get("position")).isEqualTo("absolute");
        assertThat(bar.getStyle().get("top")).isEqualTo("0");
        assertThat(bar.getStyle().get("left")).isEqualTo("0");
        assertThat(bar.getStyle().get("z-index")).isEqualTo("10");
        assertThat(bar.getStyle().get("pointer-events")).isEqualTo("none");
    }
}
