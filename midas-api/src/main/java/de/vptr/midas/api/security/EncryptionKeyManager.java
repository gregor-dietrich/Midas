package de.vptr.midas.api.security;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Manages the encryption key for data at rest.
 *
 * <p>
 * The key is resolved in the following order:
 * <ol>
 * <li>System property or environment variable {@code midas.encryption.key.path}</li>
 * <li>The default value {@code encryption.key} in the current working directory</li>
 * </ol>
 *
 * <p>
 * New keys are generated as 256-bit AES keys, Base64-encoded, and stored with 0600 permissions using an atomic move
 * operation. Existing 32-byte raw binary keys are supported for backward compatibility.
 */
@ApplicationScoped
public class EncryptionKeyManager {

    private static final Logger LOG = Logger.getLogger(EncryptionKeyManager.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int KEY_BYTES = 32;

    @ConfigProperty(name = "midas.encryption.key.path", defaultValue = "encryption.key")
    String keyPath = "encryption.key";

    @Nullable
    private SecretKey key;

    /** Returns the encryption key. */
    public synchronized SecretKey getKey() {
        if (this.key != null) {
            return this.key;
        }

        final Path path = Paths.get(this.keyPath);
        LOG.infof("Encryption key file: %s", path.toAbsolutePath());

        if (Files.exists(path)) {
            this.key = loadKey(path);
        } else {
            this.key = generateAndPersistKey(path);
        }

        return this.key;
    }

    private static SecretKey loadKey(final Path path) {
        try {
            final byte[] content = Files.readAllBytes(path);
            final byte[] keyBytes;

            if (content.length == KEY_BYTES) {
                LOG.info("Loading legacy 32-byte raw binary encryption key");
                keyBytes = content;
            } else {
                try {
                    keyBytes = Base64.getDecoder().decode(content);
                    if (keyBytes.length != KEY_BYTES) {
                        throw new IllegalStateException("Decoded key has wrong length: " + keyBytes.length + " bytes");
                    }
                    LOG.info("Encryption key loaded (Base64)");
                } catch (final IllegalArgumentException e) {
                    throw new IllegalStateException("Failed to decode Base64 encryption key", e);
                }
            }
            return new SecretKeySpec(keyBytes, "AES");
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to read encryption key from: " + path, e);
        }
    }

    private static SecretKey generateAndPersistKey(final Path path) {
        try {
            final byte[] keyBytes = new byte[KEY_BYTES];
            SECURE_RANDOM.nextBytes(keyBytes);

            final Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            // Write to a temp file in the same directory, set 0600 permissions before writing,
            // then atomically move to the target path to avoid a window of world-readable exposure.
            final Path tmp = parent != null ? Files.createTempFile(parent, ".key-", ".tmp")
                    : Files.createTempFile(".key-", ".tmp");
            try {
                try {
                    Files.setPosixFilePermissions(tmp,
                            Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
                } catch (final UnsupportedOperationException e) {
                    LOG.warnf("Cannot set POSIX permissions on %s (non-POSIX filesystem)", tmp);
                }

                Files.write(tmp, Base64.getEncoder().encode(keyBytes));

                try {
                    Files.move(tmp, path, StandardCopyOption.ATOMIC_MOVE);
                } catch (final FileAlreadyExistsException e) {
                    LOG.info("Encryption key file created concurrently; loading existing key");
                    return loadKey(path);
                }
            } finally {
                Files.deleteIfExists(tmp);
            }

            LOG.infof("Generated new encryption key at: %s", path.toAbsolutePath());
            return new SecretKeySpec(keyBytes, "AES");
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to generate and save encryption key at: " + path, e);
        }
    }
}
