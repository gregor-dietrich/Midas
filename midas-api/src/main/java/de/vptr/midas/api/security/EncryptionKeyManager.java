package de.vptr.midas.api.security;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
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

        final var path = Paths.get(this.keyPath);
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
            final var content = Files.readAllBytes(path);
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
            final var keyBytes = new byte[KEY_BYTES];
            SECURE_RANDOM.nextBytes(keyBytes);

            // Resolve the target so it always has a concrete parent directory. A relative
            // key path (the default "encryption.key") has a null parent, in which case the
            // temp file must still be created in the resolved working directory rather than
            // the system temp dir; otherwise the atomic move below crosses filesystems and
            // fails with "Invalid cross-device link" on setups like WSL2.
            final var absolutePath = path.toAbsolutePath();
            final var parent = absolutePath.getParent();
            if (parent == null) {
                throw new IllegalStateException("Cannot resolve parent directory for key path: " + path);
            }
            Files.createDirectories(parent);

            // Write to a temp file in the same directory, set 0600 permissions before
            // writing, then atomically move to the target path to avoid a window of
            // world-readable exposure.
            final var tmp = Files.createTempFile(parent, ".key-", ".tmp");
            try {
                restrictToOwner(tmp);

                Files.write(tmp, Base64.getEncoder().encode(keyBytes));

                try {
                    Files.move(tmp, absolutePath, StandardCopyOption.ATOMIC_MOVE);
                } catch (final FileAlreadyExistsException e) {
                    LOG.info("Encryption key file created concurrently; loading existing key");
                    return loadKey(absolutePath);
                } catch (final AtomicMoveNotSupportedException e) {
                    // Filesystem does not support atomic moves even within the same
                    // directory. Fall back to a regular move; the temp file already
                    // carries 0600 permissions, and we re-assert them on the target.
                    LOG.warnf("Atomic move unsupported for %s; using non-atomic move", absolutePath);
                    Files.move(tmp, absolutePath, StandardCopyOption.REPLACE_EXISTING);
                    restrictToOwner(absolutePath);
                }
            } finally {
                Files.deleteIfExists(tmp);
            }

            LOG.infof("Generated new encryption key at: %s", absolutePath);
            return new SecretKeySpec(keyBytes, "AES");
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to generate and save encryption key at: " + path, e);
        }
    }

    /**
     * Restricts a freshly-created file so only the current OS user can read or write it. Tries POSIX 0600 first; on
     * filesystems that don't support POSIX (e.g. Windows NTFS) it falls back to an ACL granting only the file owner. If
     * neither mechanism is supported, it refuses to continue — leaving an unprotected AES-256 master key on disk
     * silently is worse than a startup failure operators can fix.
     */
    private static void restrictToOwner(final Path file) throws IOException {
        try {
            Files.setPosixFilePermissions(file,
                    Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
            return;
        } catch (final UnsupportedOperationException posixUnsupported) {
            LOG.debugf("POSIX permissions unsupported for %s; trying ACL fallback", file);
        }

        final AclFileAttributeView aclView = Files.getFileAttributeView(file, AclFileAttributeView.class);
        if (aclView == null) {
            throw new IllegalStateException("Cannot restrict permissions on encryption key file " + file
                    + ". Neither POSIX nor ACL attribute views are supported by this filesystem. "
                    + "Move the key to a protected location and set midas.encryption.key.path.");
        }

        final UserPrincipal owner = Files.getOwner(file);
        final AclEntry entry = AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(owner)
                .setPermissions(EnumSet.of(AclEntryPermission.READ_DATA, AclEntryPermission.WRITE_DATA,
                        AclEntryPermission.APPEND_DATA, AclEntryPermission.READ_ATTRIBUTES,
                        AclEntryPermission.WRITE_ATTRIBUTES, AclEntryPermission.READ_ACL,
                        AclEntryPermission.SYNCHRONIZE, AclEntryPermission.DELETE))
                .build();
        aclView.setAcl(List.of(entry));
        LOG.infof("Applied owner-only ACL to encryption key file %s (POSIX unavailable)", file);
    }
}
