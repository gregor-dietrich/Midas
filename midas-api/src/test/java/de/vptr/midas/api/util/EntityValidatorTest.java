package de.vptr.midas.api.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import jakarta.validation.ValidationException;

class EntityValidatorTest {

    @Test
    void testCheckUnique_emptyOptional_noException() {
        assertDoesNotThrow(() -> EntityValidator.checkUnique(Optional.empty(), "email", "test@example.com"));
    }

    @Test
    void testCheckUnique_presentOptional_throwsValidationException() {
        final Optional<String> existing = Optional.of("existing-entity");

        final ValidationException exception = assertThrows(ValidationException.class,
                () -> EntityValidator.checkUnique(existing, "email", "test@example.com"));

        assertEquals("email 'test@example.com' already exists", exception.getMessage());
    }

    @Test
    void testCheckUnique_presentOptional_customViolationMessage() {
        final Optional<String> existing = Optional.of("existing-entity");

        final ValidationException exception = assertThrows(ValidationException.class,
                () -> EntityValidator.checkUnique(existing, "username", "admin", "is already taken"));

        assertEquals("username 'admin' is already taken", exception.getMessage());
    }

    @Test
    void testCheckUnique_messageContainsFieldNameAndValue() {
        final Optional<String> existing = Optional.of("existing-entity");

        final ValidationException exception = assertThrows(ValidationException.class,
                () -> EntityValidator.checkUnique(existing, "slug", "my-post"));

        assertTrue(exception.getMessage().contains("slug"));
        assertTrue(exception.getMessage().contains("my-post"));
    }
}
