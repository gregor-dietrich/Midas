package de.vptr.midas.gui.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

@SuppressWarnings("PMD.LinguisticNaming")
class AccountResponseDtoTest {

    @Test
    void defaultConstructor_shouldHaveDefaults() {
        // When
        final AccountResponseDto dto = new AccountResponseDto();

        // Then
        assertThat(dto.getId()).isNull();
        assertThat(dto.getName()).isNull();
        assertThat(dto.getUserCount()).isZero();
        assertThat(dto.getAssociatedUserIds()).isNull();
        assertThat(dto.getBalance()).isNull();
        assertThat(dto.isNegative()).isFalse();
    }

    @Test
    void setterGetter_shouldRoundTripAllFields() {
        // Given
        final AccountResponseDto dto = new AccountResponseDto();
        final List<Long> userIds = List.of(10L, 20L, 30L);

        // When
        dto.setId(5L);
        dto.setName("Test Account");
        dto.setUserCount(3);
        dto.setAssociatedUserIds(userIds);
        dto.setBalance(new BigDecimal("1234.56"));
        dto.setIsNegative(true);

        // Then
        assertThat(dto.getId()).isEqualTo(5L);
        assertThat(dto.getName()).isEqualTo("Test Account");
        assertThat(dto.getUserCount()).isEqualTo(3);
        assertThat(dto.getAssociatedUserIds()).containsExactly(10L, 20L, 30L);
        assertThat(dto.getBalance()).isEqualByComparingTo(new BigDecimal("1234.56"));
        assertThat(dto.isNegative()).isTrue();
    }

    @Test
    void isNegative_shouldDefaultToFalse_andBeSettable() {
        // Given
        final AccountResponseDto dto = new AccountResponseDto();

        // Then
        assertThat(dto.isNegative()).isFalse();

        // When
        dto.setIsNegative(true);

        // Then
        assertThat(dto.isNegative()).isTrue();
    }

    @Test
    void toAccountDto_shouldMapIdAndName() {
        // Given
        final AccountResponseDto dto = new AccountResponseDto();
        dto.setId(7L);
        dto.setName("Mapped Account");
        dto.setUserCount(5);
        dto.setBalance(new BigDecimal("99.99"));

        // When
        final AccountDto result = dto.toAccountDto();

        // Then
        assertThat(result.getId()).isEqualTo(7L);
        assertThat(result.getName()).isEqualTo("Mapped Account");
    }

}
