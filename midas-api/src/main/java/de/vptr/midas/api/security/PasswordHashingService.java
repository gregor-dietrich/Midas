package de.vptr.midas.api.security;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Service for hashing and verifying passwords using Bcrypt.
 */
@ApplicationScoped
public class PasswordHashingService {

    /**
     * Hashes a password using Bcrypt.
     *
     * @param password
     *            the plain text password
     * @return the hashed password
     */
    public String hashPassword(@Nullable final String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password must not be null");
        }
        return BcryptUtil.bcryptHash(password);
    }

    /**
     * Verifies a password against a stored Bcrypt hash.
     *
     * @param password
     *            the plain text password to verify
     * @param hash
     *            the stored password hash
     * @return true if the password matches, false otherwise
     */
    public boolean verifyPassword(@Nullable final String password, @Nullable final String hash) {
        if (password == null || hash == null || password.isEmpty() || hash.isEmpty()) {
            return false;
        }
        try {
            return BcryptUtil.matches(password, hash);
        } catch (final Exception e) {
            // Return false for invalid hash formats or other cryptographic errors
            return false;
        }
    }
}
