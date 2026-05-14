package de.vptr.midas.api.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jakarta.ws.rs.BadRequestException;

class UlidUtilTest {

    private static final String VALID_ULID = "01ARZ3NDEKTSV4RRFFQ69G5FAV";

    @Test
    void generate_shouldReturnNonNullString() {
        assertNotNull(UlidUtil.generate());
    }

    @Test
    @SuppressWarnings("NullAway")
    void isValid_shouldReturnFalse_whenNull() {
        assertFalse(UlidUtil.isValid(null));
    }

    @Test
    void isValid_shouldReturnTrue_whenValidUlid() {
        assertTrue(UlidUtil.isValid(VALID_ULID));
    }

    @Test
    void isValid_shouldReturnFalse_whenInvalidUlid() {
        assertFalse(UlidUtil.isValid("not-a-ulid"));
    }

    @Test
    void requireValid_shouldNotThrow_whenValidUlid() {
        assertDoesNotThrow(() -> UlidUtil.requireValid(VALID_ULID));
    }

    @Test
    void requireValid_shouldThrowBadRequestException_whenInvalidUlid() {
        assertThrows(BadRequestException.class, () -> UlidUtil.requireValid("bad"));
    }
}
