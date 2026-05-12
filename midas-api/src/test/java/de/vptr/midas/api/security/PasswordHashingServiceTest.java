package de.vptr.midas.api.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class PasswordHashingServiceTest {

    @Inject
    PasswordHashingService passwordHashingService;

    private String testPassword;

    @BeforeEach
    void setUp() {
        this.testPassword = "testPassword123";
    }

    @Test
    void testHashPassword() {
        final var hashedPassword = this.passwordHashingService.hashPassword(this.testPassword);

        assertNotNull(hashedPassword);
        assertNotEquals(this.testPassword, hashedPassword);
        assertTrue(hashedPassword.startsWith("$2a$"));
    }

    @Test
    void testVerifyPassword_happyPath() {
        final var hashedPassword = this.passwordHashingService.hashPassword(this.testPassword);
        assertTrue(this.passwordHashingService.verifyPassword(this.testPassword, hashedPassword));
    }

    @Test
    void testVerifyPassword_invalidPassword() {
        final var hashedPassword = this.passwordHashingService.hashPassword(this.testPassword);
        assertFalse(this.passwordHashingService.verifyPassword("wrongPassword", hashedPassword));
    }

    @Test
    void testVerifyPassword_invalidHash() {
        assertFalse(this.passwordHashingService.verifyPassword(this.testPassword, "invalidHash"));
    }

    @Test
    void testVerifyPassword_nullPassword() {
        final var hashedPassword = this.passwordHashingService.hashPassword(this.testPassword);
        assertFalse(this.passwordHashingService.verifyPassword(null, hashedPassword));
    }

    @Test
    void testVerifyPassword_nullHash() {
        assertFalse(this.passwordHashingService.verifyPassword(this.testPassword, null));
    }

    @Test
    void testVerifyPassword_emptyPassword() {
        final var hashedPassword = this.passwordHashingService.hashPassword(this.testPassword);
        assertFalse(this.passwordHashingService.verifyPassword("", hashedPassword));
    }

    @Test
    void testVerifyPassword_emptyHash() {
        assertFalse(this.passwordHashingService.verifyPassword(this.testPassword, ""));
    }

    @ParameterizedTest
    @CsvSource(value = { "null,hash", "password,null", "null,null" }, nullValues = "null")
    void testVerifyPassword_nullInputs(final String password, final String hash) {
        assertFalse(this.passwordHashingService.verifyPassword(password, hash));
    }

    @ParameterizedTest
    @CsvSource({ "'',hash", "' ',hash", "password,''", "password,' '", "'',''", "' ',' '" })
    void testVerifyPassword_emptyInputs(final String password, final String hash) {
        assertFalse(this.passwordHashingService.verifyPassword(password, hash));
    }
}
