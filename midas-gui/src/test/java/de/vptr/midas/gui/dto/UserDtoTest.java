package de.vptr.midas.gui.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class UserDtoTest {

    @Test
    void defaultConstructor_shouldHaveNullFields() {
        // When
        final UserDto dto = new UserDto();

        // Then
        assertThat(dto.getId()).isNull();
        assertThat(dto.getUsername()).isNull();
        assertThat(dto.getPassword()).isNull();
        assertThat(dto.getEmail()).isNull();
        assertThat(dto.getRankId()).isNull();
        assertThat(dto.getRankName()).isNull();
        assertThat(dto.getBanned()).isNull();
        assertThat(dto.getActivated()).isNull();
        assertThat(dto.getActivationKey()).isNull();
        assertThat(dto.getLastIp()).isNull();
        assertThat(dto.getCreated()).isNull();
        assertThat(dto.getLastLogin()).isNull();
        assertThat(dto.getPostsCount()).isNull();
        assertThat(dto.getCommentsCount()).isNull();
    }

    @Test
    void parameterizedConstructor_shouldSetUsernameAndEmail() {
        // When
        final UserDto dto = new UserDto("alice", "alice@example.com");

        // Then
        assertThat(dto.getUsername()).isEqualTo("alice");
        assertThat(dto.getEmail()).isEqualTo("alice@example.com");
        assertThat(dto.getId()).isNull();
    }

    @Test
    void setterGetter_shouldRoundTripAllFields() {
        // Given
        final UserDto dto = new UserDto();
        final LocalDateTime created = LocalDateTime.of(2024, 1, 1, 0, 0);
        final LocalDateTime lastLogin = LocalDateTime.of(2024, 6, 15, 12, 30);

        // When
        dto.setId(1L);
        dto.setUsername("bob");
        dto.setPassword("secret");
        dto.setRankId(2L);
        dto.setRankName("Editor");
        dto.setEmail("bob@example.com");
        dto.setBanned(false);
        dto.setActivated(true);
        dto.setActivationKey("abc-123");
        dto.setLastIp("172.16.0.1");
        dto.setCreated(created);
        dto.setLastLogin(lastLogin);
        dto.setPostsCount(50L);
        dto.setCommentsCount(120L);

        // Then
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getUsername()).isEqualTo("bob");
        assertThat(dto.getPassword()).isEqualTo("secret");
        assertThat(dto.getRankId()).isEqualTo(2L);
        assertThat(dto.getRankName()).isEqualTo("Editor");
        assertThat(dto.getEmail()).isEqualTo("bob@example.com");
        assertThat(dto.getBanned()).isFalse();
        assertThat(dto.getActivated()).isTrue();
        assertThat(dto.getActivationKey()).isEqualTo("abc-123");
        assertThat(dto.getLastIp()).isEqualTo("172.16.0.1");
        assertThat(dto.getCreated()).isEqualTo(created);
        assertThat(dto.getLastLogin()).isEqualTo(lastLogin);
        assertThat(dto.getPostsCount()).isEqualTo(50L);
        assertThat(dto.getCommentsCount()).isEqualTo(120L);
    }

    @Test
    void isActivated_withNull_shouldReturnFalse() {
        // Given
        final UserDto dto = new UserDto();

        // Then
        assertThat(dto.isActivated()).isFalse();
    }

    @Test
    void isActivated_withFalse_shouldReturnFalse() {
        // Given
        final UserDto dto = new UserDto();
        dto.setActivated(false);

        // Then
        assertThat(dto.isActivated()).isFalse();
    }

    @Test
    void isActivated_withTrue_shouldReturnTrue() {
        // Given
        final UserDto dto = new UserDto();
        dto.setActivated(true);

        // Then
        assertThat(dto.isActivated()).isTrue();
    }

    @Test
    void isBanned_withNull_shouldReturnFalse() {
        // Given
        final UserDto dto = new UserDto();

        // Then
        assertThat(dto.isBanned()).isFalse();
    }

    @Test
    void isBanned_withFalse_shouldReturnFalse() {
        // Given
        final UserDto dto = new UserDto();
        dto.setBanned(false);

        // Then
        assertThat(dto.isBanned()).isFalse();
    }

    @Test
    void isBanned_withTrue_shouldReturnTrue() {
        // Given
        final UserDto dto = new UserDto();
        dto.setBanned(true);

        // Then
        assertThat(dto.isBanned()).isTrue();
    }

    @Test
    void toString_shouldContainExpectedFields() {
        // Given
        final UserDto dto = new UserDto();
        dto.setId(1L);
        dto.setUsername("carol");
        dto.setEmail("carol@test.com");
        dto.setBanned(false);
        dto.setActivated(true);
        dto.setRankId(3L);
        dto.setRankName("User");
        dto.setPostsCount(10L);
        dto.setCommentsCount(20L);

        // When
        final String result = dto.toString();

        // Then
        assertThat(result).startsWith("User{");
        assertThat(result).contains("id=1");
        assertThat(result).contains("username='carol'");
        assertThat(result).contains("email='carol@test.com'");
        assertThat(result).contains("banned=false");
        assertThat(result).contains("activated=true");
        assertThat(result).contains("rankId=3");
        assertThat(result).contains("rankName='User'");
        assertThat(result).contains("postsCount=10");
        assertThat(result).contains("commentsCount=20");
        assertThat(result).endsWith("}");
    }
}
