package de.vptr.midas.gui.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class PostResponseDtoTest {

    @Test
    void setterGetter_shouldRoundTripAllFields() {
        // Given
        final PostResponseDto dto = new PostResponseDto();
        final LocalDateTime created = LocalDateTime.of(2024, 2, 14, 12, 0);
        final LocalDateTime lastEdit = LocalDateTime.of(2024, 2, 15, 9, 30);

        // When
        dto.setId(1L);
        dto.setTitle("Hello World");
        dto.setContent("Post content");
        dto.setUserId(10L);
        dto.setUsername("author");
        dto.setCategoryId(3L);
        dto.setCategoryName("Tech");
        dto.setPublished(true);
        dto.setCommentable(false);
        dto.setCreated(created);
        dto.setLastEdit(lastEdit);
        dto.setCommentsCount(7L);

        // Then
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getTitle()).isEqualTo("Hello World");
        assertThat(dto.getContent()).isEqualTo("Post content");
        assertThat(dto.getUserId()).isEqualTo(10L);
        assertThat(dto.getUsername()).isEqualTo("author");
        assertThat(dto.getCategoryId()).isEqualTo(3L);
        assertThat(dto.getCategoryName()).isEqualTo("Tech");
        assertThat(dto.getPublished()).isTrue();
        assertThat(dto.getCommentable()).isFalse();
        assertThat(dto.getCreated()).isEqualTo(created);
        assertThat(dto.getLastEdit()).isEqualTo(lastEdit);
        assertThat(dto.getCommentsCount()).isEqualTo(7L);
    }

    @Test
    void toPostDto_shouldMapAllFields() {
        // Given
        final PostResponseDto dto = new PostResponseDto();
        final LocalDateTime created = LocalDateTime.of(2024, 5, 1, 8, 0);
        final LocalDateTime lastEdit = LocalDateTime.of(2024, 5, 2, 10, 0);
        dto.setId(99L);
        dto.setTitle("Guide");
        dto.setContent("Full guide content");
        dto.setUserId(5L);
        dto.setUsername("writer");
        dto.setCategoryId(2L);
        dto.setCategoryName("Tutorials");
        dto.setPublished(true);
        dto.setCommentable(true);
        dto.setCreated(created);
        dto.setLastEdit(lastEdit);
        dto.setCommentsCount(42L);

        // When
        final PostDto result = dto.toPostDto();

        // Then
        assertThat(result.getId()).isEqualTo(99L);
        assertThat(result.getTitle()).isEqualTo("Guide");
        assertThat(result.getContent()).isEqualTo("Full guide content");
        assertThat(result.getUserId()).isEqualTo(5L);
        assertThat(result.getUsername()).isEqualTo("writer");
        assertThat(result.getCategoryId()).isEqualTo(2L);
        assertThat(result.getCategoryName()).isEqualTo("Tutorials");
        assertThat(result.getPublished()).isTrue();
        assertThat(result.getCommentable()).isTrue();
        assertThat(result.getCreated()).isEqualTo(created);
        assertThat(result.getLastEdit()).isEqualTo(lastEdit);
        assertThat(result.getCommentsCount()).isEqualTo(42L);
    }

    @Test
    void toString_shouldContainExpectedFields() {
        // Given
        final PostResponseDto dto = new PostResponseDto();
        dto.setId(1L);
        dto.setTitle("My Post");
        dto.setUsername("alice");
        dto.setCategoryName("General");
        dto.setPublished(true);
        dto.setCommentsCount(3L);

        // When
        final String result = dto.toString();

        // Then
        assertThat(result).startsWith("PostResponseDto{");
        assertThat(result).contains("id=1");
        assertThat(result).contains("title='My Post'");
        assertThat(result).contains("username='alice'");
        assertThat(result).contains("categoryName='General'");
        assertThat(result).contains("published=true");
        assertThat(result).contains("commentsCount=3");
        assertThat(result).endsWith("}");
    }
}
