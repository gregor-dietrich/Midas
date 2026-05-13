package de.vptr.midas.api.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class EncryptionKeyManagerTest {

    @Inject
    EncryptionKeyManager keyManager;

    @Test
    void testGetKey() {
        final SecretKey key1 = this.keyManager.getKey();
        assertNotNull(key1);
        final SecretKey key2 = this.keyManager.getKey();
        assertSame(key1, key2);
    }
}
