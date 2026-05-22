package de.vptr.midas.gui.service;

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

            // Resolve the target so it always has a concrete parent directory. A relative
            // key path (the default "session.key") has a null parent, in which case the
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
                    LOG.info("Session key file created concurrently; loading existing key");
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

            LOG.infof("Generated new session key at: %s", absolutePath);
            return new SecretKeySpec(keyBytes, "AES");
        } catch (final IOException e) {
            throw new IllegalStateException("Failed to generate and save session key at: " + path, e);
        }
    }

    /**
     * Restricts a freshly-created file so only the current OS user can read or write it. Tries POSIX 0600 first; on
     * filesystems that don't support POSIX (e.g. Windows NTFS) it falls back to an ACL granting only the file owner. If
     * neither mechanism is supported, it refuses to continue — leaving an unprotected session key on disk silently is
     * worse than a startup failure operators can fix.
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
            throw new IllegalStateException("Cannot restrict permissions on session key file " + file
                    + ". Neither POSIX nor ACL attribute views are supported by this filesystem. "
                    + "Move the key to a protected location and set midas.session.key.path.");
        }

        final UserPrincipal owner = Files.getOwner(file);
        final AclEntry entry = AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(owner)
                .setPermissions(EnumSet.of(AclEntryPermission.READ_DATA, AclEntryPermission.WRITE_DATA,
                        AclEntryPermission.APPEND_DATA, AclEntryPermission.READ_ATTRIBUTES,
                        AclEntryPermission.WRITE_ATTRIBUTES, AclEntryPermission.READ_ACL,
                        AclEntryPermission.SYNCHRONIZE, AclEntryPermission.DELETE))
                .build();
        aclView.setAcl(List.of(entry));
        LOG.infof("Applied owner-only ACL to session key file %s (POSIX unavailable)", file);
    }
}
