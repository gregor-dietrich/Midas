package de.vptr.midas.api.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class DtoSerializationTest {

    @Test
    void testPasswordChangeDtoConstructor() {
        final var dto = new PasswordChangeDto("oldPassword", "newPassword");
        assertEquals("oldPassword", dto.currentPassword);
        assertEquals("newPassword", dto.newPassword);
    }

    @Test
    void testPasswordChangeDtoDefaultConstructor() {
        final var dto = new PasswordChangeDto();
        assertNotNull(dto);
    }

    @Test
    void testAccountAssociationResponseDto() {
        final var dto = new AccountAssociationResponseDto(true);
        assertEquals(true, dto.associated());
    }

    @Test
    void testAccountAssociationResponseDtoFalse() {
        final var dto = new AccountAssociationResponseDto(false);
        assertEquals(false, dto.associated());
    }

    @Test
    void testAccountAccessResponseDto() {
        final var dto = new AccountAccessResponseDto(true);
        assertEquals(true, dto.hasAccess());
    }

    @Test
    void testAccountAccessResponseDtoFalse() {
        final var dto = new AccountAccessResponseDto(false);
        assertEquals(false, dto.hasAccess());
    }
}
