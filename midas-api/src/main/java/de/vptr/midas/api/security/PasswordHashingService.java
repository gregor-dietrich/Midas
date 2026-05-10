package de.vptr.midas.api.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.jboss.logging.Logger;

import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;

/** Type definition. */
@ApplicationScoped
public class PasswordHashingService {

    private static final Logger LOG = Logger.getLogger(PasswordHashingService.class);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int PBKDF2_ITERATIONS = 100000; // OWASP recommended minimum
    private static final int SALT_LENGTH = 32; // 32 bytes = 256 bits
    private static final int HASH_LENGTH = 8 * SALT_LENGTH; // 256 bits

    /**
     * Generates a random salt
     * 
     * @return Base64 encoded salt
     */
    public String generateSalt() {
        final var salt = new byte[SALT_LENGTH];
        SECURE_RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Hashes a password using PBKDF2 with SHA-256
     * 
     * @param password
     *            the plain text password
     * @param salt
     *            the salt as a Base64 encoded string
     * @return the hashed password as a Base64 encoded string
     */
    public String hashPassword(@Nullable final String password, final String salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        final var passwordChars = password != null ? password.toCharArray() : null;
        try {
            return this.hashPasswordChars(passwordChars, salt);
        } finally {
            if (passwordChars != null) {
                Arrays.fill(passwordChars, '\0');
            }
        }
    }

    /**
     * Hashes a password char array using PBKDF2 with SHA-256.
     * 
     * @param password
     *            the plain text password chars
     * @param salt
     *            the salt as a Base64 encoded string
     * @return the hashed password as a Base64 encoded string
     */
    public String hashPasswordChars(@Nullable final char[] password, final String salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (password == null) {
            throw new InvalidKeySpecException("Password must not be null");
        }
        final var saltBytes = Base64.getDecoder().decode(salt);

        final var spec = new PBEKeySpec(password, saltBytes, PBKDF2_ITERATIONS, HASH_LENGTH);

        try {
            final var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            final var hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * Verifies a password against a stored hash
     * 
     * @param providedPassword
     *            the plain text password to verify
     * @param storedHash
     *            the stored password hash
     * @param salt
     *            the salt used for hashing
     * @return true if the password matches, false otherwise
     */
    public boolean verifyPassword(@Nullable final String providedPassword, @Nullable final String storedHash,
            @Nullable final String salt) {
        if (providedPassword == null || storedHash == null || salt == null) {
            return false;
        }

        if (providedPassword.isEmpty() || storedHash.isEmpty() || salt.isEmpty()) {
            return false;
        }

        final var providedPasswordChars = providedPassword.toCharArray();
        try {
            return this.verifyPasswordChars(providedPasswordChars, storedHash, salt);
        } finally {
            Arrays.fill(providedPasswordChars, '\0');
        }
    }

    /**
     * Verifies password chars against a stored hash.
     * 
     * @param providedPassword
     *            the plain text password chars to verify
     * @param storedHash
     *            the stored password hash
     * @param salt
     *            the salt used for hashing
     * @return true if the password matches, false otherwise
     */
    public boolean verifyPasswordChars(@Nullable final char[] providedPassword, final String storedHash,
            final String salt) {
        if (providedPassword == null || storedHash == null || salt == null) {
            return false;
        }

        if (providedPassword.length == 0 || storedHash.isEmpty() || salt.isEmpty()) {
            return false;
        }

        try {
            final var hashedProvidedPassword = this.hashPasswordChars(providedPassword, salt);
            return MessageDigest.isEqual(hashedProvidedPassword.getBytes(StandardCharsets.UTF_8),
                    storedHash.getBytes(StandardCharsets.UTF_8));
        } catch (final NoSuchAlgorithmException | InvalidKeySpecException e) {
            LOG.debug("Password verification failed", e);
            return false;
        } catch (final IllegalArgumentException e) {
            LOG.debug("Invalid Base64 in salt", e);
            return false;
        }
    }
}
