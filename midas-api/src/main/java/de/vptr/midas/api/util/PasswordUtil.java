package de.vptr.midas.api.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import de.vptr.midas.api.security.PasswordHashingService;
import jakarta.validation.ValidationException;

/**
 * Standalone CLI utility that prints the bcrypt hash of a password, e.g. for {@code db/demo/R__demo_data.sql}. Invoked
 * by {@code scripts/password.sh}. The password is read as the first line of stdin, never from the command line, so
 * {@code ps} does not show it and whitespace inside it survives.
 */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    /**
     * CLI entrypoint for password hash generation.
     *
     * @param args
     *            unused; the password comes from stdin
     */
    public static void main(final String[] args) throws IOException {
        final String password = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8)).readLine();
        if (password == null || password.isEmpty()) {
            System.err.println("Usage: pass the password as the first line of stdin");
            System.exit(1);
        }

        try {
            System.out.println("Password: " + new PasswordHashingService().hashPassword(password));
        } catch (final ValidationException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}
