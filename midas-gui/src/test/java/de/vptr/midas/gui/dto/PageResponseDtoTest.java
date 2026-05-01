package de.vptr.midas.gui.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PageResponseDtoTest {

    @Test
    void setterGetter_shouldRoundTripAllFields() {
        // Given
        final PageResponseDto dto = new PageResponseDto();

        // When
        dto.setId(1L);
        dto.setTitle("About Us");
        dto.setContent("Page content here");

        // Then
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getTitle()).isEqualTo("About Us");
        assertThat(dto.getContent()).isEqualTo("Page content here");
    }

    @Test
    void toPageDto_shouldMapAllFields() {
        // Given
        final PageResponseDto dto = new PageResponseDto();
        dto.setId(5L);
        dto.setTitle("Contact");
        dto.setContent("Contact page content");

        // When
        final PageDto result = dto.toPageDto();

        // Then
        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getTitle()).isEqualTo("Contact");
        assertThat(result.getContent()).isEqualTo("Contact page content");
    }

}
