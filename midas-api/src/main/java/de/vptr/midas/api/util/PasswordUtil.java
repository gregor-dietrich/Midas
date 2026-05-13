package de.vptr.midas.api.util;

import io.quarkus.elytron.security.common.BcryptUtil;

/**
 * Standalone CLI utility for generating password hashes during initialization. Invoked by {@code scripts/password.sh}
 * to generate credentials for {@code sql/init.sql}.
 *
 * Usage: generate &lt;password&gt; Outputs: hash suitable for insertion into init.sql.
 */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    /**
     * CLI entrypoint for password hash generation.
     *
     * @param args
     *            CLI arguments; expected: {@code generate <password>}
     */
    public static void main(final String[] args) {
        if (args.length < 2 || !"generate".equals(args[0])) {
            System.err.println("Usage: generate <password>");
            System.exit(1);
        }

        final String password = args[1];
        final String hash = BcryptUtil.bcryptHash(password);

        System.out.println("Password: " + hash);
    }
}
