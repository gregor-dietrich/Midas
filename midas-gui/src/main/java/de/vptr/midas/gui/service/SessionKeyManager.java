package de.vptr.midas.gui.service;

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
 * Manages the session encryption key used by {@link SessionAuthorizationStore}.
 *
 * <p>
 * The key is persisted to a file so it survives application restarts, enabling horizontal scaling without session loss.
 * Resolution order:
 * <ol>
 * <li>System property or environment variable {@code midas.session.key.path}</li>
 * <li>Default value {@code session.key} in the current working directory</li>
 * </ol>
 */
@ApplicationScoped
public class SessionKeyManager {

    private static final Logger LOG = Logger.getLogger(SessionKeyManager.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int KEY_BYTES = 32;

    @ConfigProperty(name = "midas.session.key.path", defaultValue = "session.key")
    String keyPath = "session.key";

    @Nullable
    private SecretKey key;

    /** Returns the session encryption key, loading or generating it on first access. */
    public synchronized SecretKey getKey() {
        if (this.key != null) {
            return this.key;
        }

        final var path = Paths.get(this.keyPath);
        LOG.infof("Session key file: %s", path.toAbsolutePath());

        if (Files.exists(path)) {
            this.key = loadKey(path);
        } else {
            this.key = generateAndPersistKey(path);
        }

        return this.key;
    }

    private static SecretKey loadKey(final Path path) {
        try {
            final var content = Files.readAllBytes(path);
            final byte[] keyBytes;
            try {
                keyBytes = Base64.getDecoder().decode(content);
                if (keyBytes.length != KEY_BYTES) {
                    throw new IllegalStateException(
                            "Decoded session key has wrong length: " + keyBytes.length + " bytes");
                }
            } catch (final IllegalArgumentException e) {
                throw new IllegalStateException("Failed to decode Base64 session key", e);
            }
            LOG.info("Session key loaded");
            return new SecretKeySpec(keyBytes, "AES");
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to read session key from: " + path, e);
        }
    }

    private static SecretKey generateAndPersistKey(final Path path) {
        try {
            final var keyBytes = new byte[KEY_BYTES];
            SECURE_RANDOM.nextBytes(keyBytes);

            final var parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            final var tmp = parent != null ? Files.createTempFile(parent, ".key-", ".tmp")
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
                    LOG.info("Session key file created concurrently; loading existing key");
                    return loadKey(path);
                }
            } finally {
                Files.deleteIfExists(tmp);
            }

            LOG.infof("Generated new session key at: %s", path.toAbsolutePath());
            return new SecretKeySpec(keyBytes, "AES");
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to generate and save session key at: " + path, e);
        }
    }
}
