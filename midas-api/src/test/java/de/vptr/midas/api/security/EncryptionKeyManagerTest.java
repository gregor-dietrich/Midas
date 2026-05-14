package de.vptr.midas.api.security;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EncryptionKeyManagerTest {

    private EncryptionKeyManager keyManager;

    @TempDir
    Path tempDir;

    private Path keyFile;

    @BeforeEach
    void setUp() {
        this.keyManager = new EncryptionKeyManager();
        this.keyFile = this.tempDir.resolve("encryption.key");
        this.keyManager.keyPath = this.keyFile.toString();
    }

    @Test
    void testGetKey_GeneratesNewKey() throws IOException {
        final SecretKey key = this.keyManager.getKey();
        assertNotNull(key);
        assertEquals("AES", key.getAlgorithm());
        assertEquals(32, key.getEncoded().length);

        assertTrue(Files.exists(this.keyFile));
        final byte[] content = Files.readAllBytes(this.keyFile);
        final byte[] decoded = Base64.getDecoder().decode(content);
        assertArrayEquals(key.getEncoded(), decoded);
    }

    @Test
    void testGetKey_LoadsExistingBase64Key() throws IOException {
        final byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);
        Files.write(this.keyFile, Base64.getEncoder().encode(keyBytes));

        final SecretKey key = this.keyManager.getKey();
        assertNotNull(key);
        assertArrayEquals(keyBytes, key.getEncoded());
    }

    @Test
    void testGetKey_LoadsLegacyBinaryKey() throws IOException {
        final byte[] keyBytes = new byte[32];
        new SecureRandom().nextBytes(keyBytes);
        Files.write(this.keyFile, keyBytes);

        final SecretKey key = this.keyManager.getKey();
        assertNotNull(key);
        assertArrayEquals(keyBytes, key.getEncoded());
    }

    @Test
    void testGetKey_ReturnsSameInstance() {
        final SecretKey key1 = this.keyManager.getKey();
        final SecretKey key2 = this.keyManager.getKey();
        assertSame(key1, key2);
    }
}
