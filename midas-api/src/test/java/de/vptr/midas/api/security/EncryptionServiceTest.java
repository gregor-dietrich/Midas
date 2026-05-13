package de.vptr.midas.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class EncryptionServiceTest {

    @Inject
    EncryptionService encryptionService;

    @Test
    void testEncryptDecrypt() {
        final String plainText = "Hello World!";
        final String encrypted = this.encryptionService.encrypt(plainText);

        assertNotNull(encrypted);
        assertTrue(encrypted.startsWith("1|"));
        assertNotEquals(plainText, encrypted);

        final String decrypted = this.encryptionService.decrypt(encrypted);
        assertEquals(plainText, decrypted);
    }

    @Test
    void testEncryptDecryptNull() {
        assertNull(this.encryptionService.encrypt(null));
        assertNull(this.encryptionService.decrypt(null));
    }

    @Test
    void testDecryptInvalidFormat() {
        final String invalid = "invalid-format";
        assertEquals(invalid, this.encryptionService.decrypt(invalid));

        final String wrongVersion = "2|base64|base64";
        assertEquals(wrongVersion, this.encryptionService.decrypt(wrongVersion));
    }

    @Test
    void testGenerateBlindIndex() {
        final String plainText = "test@example.com";
        final String index1 = this.encryptionService.generateBlindIndex(plainText);
        final String index2 = this.encryptionService.generateBlindIndex(plainText);

        assertNotNull(index1);
        assertEquals(index1, index2);

        final String index3 = this.encryptionService.generateBlindIndex("other@example.com");
        assertNotEquals(index1, index3);
    }

    @Test
    void testGenerateBlindIndexNull() {
        assertNull(this.encryptionService.generateBlindIndex(null));
    }
}
