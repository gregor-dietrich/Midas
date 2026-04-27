package de.vptr.midas.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
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
    private String testSalt;

    @BeforeEach
    void setUp() {
        this.testPassword = "testPassword123";
        this.testSalt = this.passwordHashingService.generateSalt();
    }

    @RepeatedTest(10)
    void testGenerateSalt() {
        final var salt1 = this.passwordHashingService.generateSalt();
        final var salt2 = this.passwordHashingService.generateSalt();

        assertNotNull(salt1);
        assertNotNull(salt2);
        assertNotEquals(salt1, salt2);
        assertTrue(salt1.length() > 0);
        assertTrue(salt2.length() > 0);
    }

    @Test
    void testHashPassword() throws NoSuchAlgorithmException, InvalidKeySpecException {
        final var hashedPassword = this.passwordHashingService.hashPassword(this.testPassword, this.testSalt);

        assertNotNull(hashedPassword);
        assertNotEquals(this.testPassword, hashedPassword);
        assertTrue(hashedPassword.length() > 0);
    }

    void testHashPassword_consistency() throws NoSuchAlgorithmException, InvalidKeySpecException {
        final var hash1 = this.passwordHashingService.hashPassword(this.testPassword, this.testSalt);
        final var hash2 = this.passwordHashingService.hashPassword(this.testPassword, this.testSalt);

        assertEquals(hash1, hash2);
    }

    @RepeatedTest(10)
    void testHashPassword_differentSalts() throws NoSuchAlgorithmException, InvalidKeySpecException {
        final var salt1 = this.passwordHashingService.generateSalt();
        final var salt2 = this.passwordHashingService.generateSalt();

        final var hash1 = this.passwordHashingService.hashPassword(this.testPassword, salt1);
        final var hash2 = this.passwordHashingService.hashPassword(this.testPassword, salt2);

        assertNotEquals(hash1, hash2);
    }

    @Test
    void testVerifyPassword_happyPath() throws NoSuchAlgorithmException, InvalidKeySpecException {
        final var hashedPassword = this.passwordHashingService.hashPassword(this.testPassword, this.testSalt);
        assertTrue(this.passwordHashingService.verifyPassword(this.testPassword, hashedPassword, this.testSalt));
    }

    @Test
    void testVerifyPassword_invalidPassword() throws NoSuchAlgorithmException, InvalidKeySpecException {
        final var hashedPassword = this.passwordHashingService.hashPassword(this.testPassword, this.testSalt);
        assertFalse(this.passwordHashingService.verifyPassword("wrongPassword", hashedPassword, this.testSalt));
    }

    @Test
    void testVerifyPassword_invalidHash() {
        assertFalse(this.passwordHashingService.verifyPassword(this.testPassword, "invalidHash", this.testSalt));
    }

    @Test
    void testVerifyPassword_invalidSalt() throws NoSuchAlgorithmException, InvalidKeySpecException {
        final var hashedPassword = this.passwordHashingService.hashPassword(this.testPassword, this.testSalt);
        assertFalse(this.passwordHashingService.verifyPassword(this.testPassword, hashedPassword, "invalidSalt"));
    }

    @Test
    void testVerifyPassword_nullPassword() throws NoSuchAlgorithmException, InvalidKeySpecException {
        final var hashedPassword = this.passwordHashingService.hashPassword(this.testPassword, this.testSalt);
        assertFalse(this.passwordHashingService.verifyPassword(null, hashedPassword, this.testSalt));
    }

    @Test
    void testVerifyPassword_nullHash() {
        assertFalse(this.passwordHashingService.verifyPassword(this.testPassword, null, this.testSalt));
    }

    @Test
    void testVerifyPassword_nullSalt() throws NoSuchAlgorithmException, InvalidKeySpecException {
        final var hashedPassword = this.passwordHashingService.hashPassword(this.testPassword, this.testSalt);
        assertFalse(this.passwordHashingService.verifyPassword(this.testPassword, hashedPassword, null));
    }

    @Test
    void testVerifyPassword_emptyPassword() throws NoSuchAlgorithmException, InvalidKeySpecException {
        final var hashedPassword = this.passwordHashingService.hashPassword(this.testPassword, this.testSalt);
        assertFalse(this.passwordHashingService.verifyPassword("", hashedPassword, this.testSalt));
    }

    @Test
    void testVerifyPassword_emptyHash() {
        assertFalse(this.passwordHashingService.verifyPassword(this.testPassword, "", this.testSalt));
    }

    @Test
    void testVerifyPassword_emptySalt() throws NoSuchAlgorithmException, InvalidKeySpecException {
        final var hashedPassword = this.passwordHashingService.hashPassword(this.testPassword, this.testSalt);
        assertFalse(this.passwordHashingService.verifyPassword(this.testPassword, hashedPassword, ""));
    }

    @ParameterizedTest
    @CsvSource(value = {
            "null,hash,salt",
            "password,null,salt",
            "password,hash,null",
            "password,null,null",
            "null,hash,null",
            "null,null,salt",
            "null,null,null"
    }, nullValues = "null")
    void testVerifyPassword_nullInputs(final String password, final String hash, final String salt) {
        assertFalse(this.passwordHashingService.verifyPassword(password, hash, salt));
    }

    @ParameterizedTest
    @CsvSource({
            "'',hash,salt",
            "' ',hash,salt",
            "password,'',salt",
            "password,' ',salt",
            "password,hash,''",
            "password,hash,' '",
            "password,'',''",
            "password,' ',' '",
            "'',hash,''",
            "' ',hash,' '",
            "'','',salt",
            "' ',' ',salt",
            "'','',''",
            "' ',' ',' '"
    })
    void testVerifyPassword_emptyInputs(final String password, final String hash, final String salt) {
        assertFalse(this.passwordHashingService.verifyPassword(password, hash, salt));
    }
}