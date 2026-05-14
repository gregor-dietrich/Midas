package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SuppressWarnings("PMD.LinguisticNaming")
class SessionKeyManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void getKey_shouldGenerateAndReturnKey() {
        final var manager = new SessionKeyManager();
        manager.keyPath = tempDir.resolve("session.key").toString();

        final var key = manager.getKey();

        assertThat(key).isNotNull();
        assertThat(key.getAlgorithm()).isEqualTo("AES");
        assertThat(key.getEncoded()).hasSize(32);
    }

    @Test
    void getKey_shouldReturnSameKeyOnSubsequentCalls() {
        final var manager = new SessionKeyManager();
        manager.keyPath = tempDir.resolve("session.key").toString();

        final var key1 = manager.getKey();
        final var key2 = manager.getKey();

        assertThat(key1.getEncoded()).isEqualTo(key2.getEncoded());
    }

    @Test
    void getKey_shouldPersistKeyToFile() throws Exception {
        final var keyFile = tempDir.resolve("session.key");
        final var manager = new SessionKeyManager();
        manager.keyPath = keyFile.toString();

        manager.getKey();

        assertThat(Files.exists(keyFile)).isTrue();
    }

    @Test
    void getKey_shouldLoadExistingKeyFile() throws Exception {
        final var keyFile = tempDir.resolve("session.key");
        final var manager1 = new SessionKeyManager();
        manager1.keyPath = keyFile.toString();

        final var originalKey = manager1.getKey();

        final var manager2 = new SessionKeyManager();
        manager2.keyPath = keyFile.toString();
        final var loadedKey = manager2.getKey();

        assertThat(loadedKey.getEncoded()).isEqualTo(originalKey.getEncoded());
    }
}
