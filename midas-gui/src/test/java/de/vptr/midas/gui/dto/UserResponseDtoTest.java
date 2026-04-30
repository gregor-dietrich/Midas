package de.vptr.midas.gui.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class UserResponseDtoTest {

    @Test
    void setterGetter_shouldRoundTripAllFields() {
        // Given
        final UserResponseDto dto = new UserResponseDto();
        final LocalDateTime created = LocalDateTime.of(2024, 1, 15, 10, 30);
        final LocalDateTime lastLogin = LocalDateTime.of(2024, 6, 20, 14, 0);

        // When
        dto.setId(42L);
        dto.setUsername("testuser");
        dto.setEmail("test@example.com");
        dto.setRankId(3L);
        dto.setRankName("Moderator");
        dto.setBanned(false);
        dto.setActivated(true);
        dto.setLastIp("192.168.1.1");
        dto.setCreated(created);
        dto.setLastLogin(lastLogin);
        dto.setPostsCount(15L);
        dto.setCommentsCount(42L);

        // Then
        assertThat(dto.getId()).isEqualTo(42L);
        assertThat(dto.getUsername()).isEqualTo("testuser");
        assertThat(dto.getEmail()).isEqualTo("test@example.com");
        assertThat(dto.getRankId()).isEqualTo(3L);
        assertThat(dto.getRankName()).isEqualTo("Moderator");
        assertThat(dto.getBanned()).isFalse();
        assertThat(dto.getActivated()).isTrue();
        assertThat(dto.getLastIp()).isEqualTo("192.168.1.1");
        assertThat(dto.getCreated()).isEqualTo(created);
        assertThat(dto.getLastLogin()).isEqualTo(lastLogin);
        assertThat(dto.getPostsCount()).isEqualTo(15L);
        assertThat(dto.getCommentsCount()).isEqualTo(42L);
    }

    @Test
    void toUserDto_shouldMapAllFields() {
        // Given
        final UserResponseDto dto = new UserResponseDto();
        final LocalDateTime created = LocalDateTime.of(2024, 3, 1, 8, 0);
        final LocalDateTime lastLogin = LocalDateTime.of(2024, 3, 10, 16, 45);
        dto.setId(10L);
        dto.setUsername("admin");
        dto.setEmail("admin@example.com");
        dto.setRankId(1L);
        dto.setRankName("Admin");
        dto.setBanned(false);
        dto.setActivated(true);
        dto.setLastIp("10.0.0.1");
        dto.setCreated(created);
        dto.setLastLogin(lastLogin);
        dto.setPostsCount(100L);
        dto.setCommentsCount(200L);

        // When
        final UserDto result = dto.toUserDto();

        // Then
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getUsername()).isEqualTo("admin");
        assertThat(result.getEmail()).isEqualTo("admin@example.com");
        assertThat(result.getRankId()).isEqualTo(1L);
        assertThat(result.getRankName()).isEqualTo("Admin");
        assertThat(result.getBanned()).isFalse();
        assertThat(result.getActivated()).isTrue();
        assertThat(result.getLastIp()).isEqualTo("10.0.0.1");
        assertThat(result.getCreated()).isEqualTo(created);
        assertThat(result.getLastLogin()).isEqualTo(lastLogin);
        assertThat(result.getPostsCount()).isEqualTo(100L);
        assertThat(result.getCommentsCount()).isEqualTo(200L);
    }

    @Test
    void isNegative_shouldDefaultToFalse_andBeSettable() {
        // Given
        final UserResponseDto dto = new UserResponseDto();

        // Then
        assertThat(dto.isNegative()).isFalse();

        // When
        dto.setIsNegative(true);

        // Then
        assertThat(dto.isNegative()).isTrue();
    }

    @Test
    void toString_shouldContainExpectedFields() {
        // Given
        final UserResponseDto dto = new UserResponseDto();
        dto.setId(1L);
        dto.setUsername("bob");
        dto.setEmail("bob@test.com");
        dto.setRankName("User");
        dto.setBanned(false);
        dto.setActivated(true);
        dto.setPostsCount(5L);
        dto.setCommentsCount(12L);

        // When
        final String result = dto.toString();

        // Then
        assertThat(result).startsWith("UserResponseDto{");
        assertThat(result).contains("id=1");
        assertThat(result).contains("username='bob'");
        assertThat(result).contains("email='bob@test.com'");
        assertThat(result).contains("rankName='User'");
        assertThat(result).contains("banned=false");
        assertThat(result).contains("activated=true");
        assertThat(result).contains("postsCount=5");
        assertThat(result).contains("commentsCount=12");
        assertThat(result).endsWith("}");
    }
}
