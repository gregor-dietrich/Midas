package de.vptr.midas.api.security;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.SecureRandom;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;

/** Manages the encryption key for data at rest. */
@ApplicationScoped
public class EncryptionKeyManager {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @ConfigProperty(name = "midas.encryption.key.path", defaultValue = "encryption.key")
    String keyPath = "encryption.key";

    @Nullable
    private SecretKey key;

    /** Returns the encryption key. */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "SecretKeySpec is effectively immutable")
    public synchronized SecretKey getKey() {
        if (this.key != null) {
            return this.key;
        }
        try {
            final File keyFile = new File(this.keyPath);
            if (keyFile.exists()) {
                final byte[] keyBytes = Files.readAllBytes(keyFile.toPath());
                this.key = new SecretKeySpec(keyBytes, "AES");
            } else {
                final byte[] newKey = new byte[32];
                SECURE_RANDOM.nextBytes(newKey);
                Files.write(keyFile.toPath(), newKey);
                this.key = new SecretKeySpec(newKey, "AES");
            }
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to load or generate encryption key", e);
        }
        return this.key;
    }
}
