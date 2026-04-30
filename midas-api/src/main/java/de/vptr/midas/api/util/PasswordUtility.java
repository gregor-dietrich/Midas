package de.vptr.midas.api.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Standalone CLI utility for generating password salts and hashes during initialization.
 * Invoked by {@code scripts/password.sh} to generate credentials for {@code sql/init.sql}.
 *
 * <p>Note: This class replicates PBKDF2-SHA256 hashing logic from
 * {@link de.vptr.midas.api.security.PasswordHashingService} because it must be usable as a
 * standalone CLI tool without loading the full application context. Not for production use.
 *
 * Usage: generate &lt;password&gt;
 * Outputs: salt and hash suitable for insertion into init.sql.
 */
public class PasswordUtility {

    private static final SecureRandom random = new SecureRandom();

    private static final int PBKDF2_ITERATIONS = 100000;
    private static final int SALT_LENGTH = 32;
    private static final int HASH_LENGTH = 8 * SALT_LENGTH;

    private PasswordUtility() {
    }

    /**
     * CLI entrypoint for password salt and hash generation.
     *
     * @param args CLI arguments; expected: {@code generate <password>}
     * @throws NoSuchAlgorithmException when the PBKDF2 algorithm is unavailable
     * @throws InvalidKeySpecException  when the key spec cannot be processed
     */
    public static void main(final String[] args) throws NoSuchAlgorithmException, InvalidKeySpecException {
        if (args.length < 2 || !"generate".equals(args[0])) {
            System.err.println("Usage: generate <password>");
            System.exit(1);
        }

        final String password = args[1];
        final String salt = generateSalt();
        final String hash = hashPassword(password, salt);

        System.out.println("Salt:     " + salt);
        System.out.println("Password: " + hash);
    }

    private static String generateSalt() {
        final var saltBytes = new byte[SALT_LENGTH];
        random.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    private static String hashPassword(final String password, final String salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        final var saltBytes = Base64.getDecoder().decode(salt);
        final var spec = new PBEKeySpec(password.toCharArray(), saltBytes, PBKDF2_ITERATIONS, HASH_LENGTH);
        try {
            final var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            final var hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } finally {
            spec.clearPassword();
        }
    }
}
