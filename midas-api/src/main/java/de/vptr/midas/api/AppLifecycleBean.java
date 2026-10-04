package de.vptr.midas.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import de.vptr.midas.api.service.UserService;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/** Application lifecycle management. */
@ApplicationScoped
public class AppLifecycleBean {

    private static final Logger LOG = Logger.getLogger(AppLifecycleBean.class);

    private static final String DEFAULT_DB_PASSWORD = "changeit";

    private final LaunchMode launchMode;

    private final String dbPassword;

    private final UserService userService;

    private final String bootstrapAdminUsername;

    private final Optional<String> bootstrapAdminPassword;

    @Inject
    AppLifecycleBean(final LaunchMode launchMode,
            @ConfigProperty(name = "quarkus.datasource.password", defaultValue = "") final String dbPassword,
            final UserService userService,
            @ConfigProperty(name = "app.bootstrap.admin-username") final String bootstrapAdminUsername,
            @ConfigProperty(name = "app.bootstrap.admin-password") final Optional<String> bootstrapAdminPassword) {
        this.launchMode = launchMode;
        this.dbPassword = dbPassword;
        this.userService = userService;
        this.bootstrapAdminUsername = bootstrapAdminUsername;
        this.bootstrapAdminPassword = bootstrapAdminPassword;
    }

    void onStart(@Observes final StartupEvent ev) {
        this.failOnDefaultDbPasswordInProduction();
        this.bootstrapAdminInProduction();

        final String banner = readBanner();
        if (banner != null) {
            LOG.info(banner);
        }
        LOG.info("Welcome to Midas API! o/");
    }

    /**
     * Aborts startup if the default database password is still in use in a production launch. Dev and test modes are
     * exempt, since they intentionally use the throwaway dev-services credentials.
     */
    private void failOnDefaultDbPasswordInProduction() {
        if (this.launchMode == LaunchMode.NORMAL && DEFAULT_DB_PASSWORD.equals(this.dbPassword)) {
            LOG.errorf("FATAL: the default database password '%s' is in use in production!", DEFAULT_DB_PASSWORD);
            LOG.error("Set the QUARKUS_DATASOURCE_PASSWORD environment variable to a strong secret before deploying.");
            throw new IllegalStateException("Default database password in use in production");
        }
    }

    /**
     * Creates the initial admin from configuration when a production launch finds no user account, and aborts startup
     * if no bootstrap password is configured then. Production has no seeded account; dev and test load demo accounts.
     */
    private void bootstrapAdminInProduction() {
        if (this.launchMode != LaunchMode.NORMAL || this.userService.hasUsers()) {
            return;
        }
        final String password = this.bootstrapAdminPassword.orElse(null);
        if (password == null) {
            LOG.error("FATAL: no user account exists, so there is no admin to log in with!");
            LOG.error("Set the APP_BOOTSTRAP_ADMIN_PASSWORD environment variable to a strong password.");
            throw new IllegalStateException("No user account exists and no bootstrap admin password is configured");
        }
        if (this.userService.createInitialAdmin(this.bootstrapAdminUsername, password)) {
            LOG.infof(
                    "Created the initial admin account '%s'. APP_BOOTSTRAP_ADMIN_PASSWORD is no longer needed and can "
                            + "be removed from the configuration.",
                    this.bootstrapAdminUsername);
        }
    }

    void onStop(@Observes final ShutdownEvent ev) {
        LOG.info("Midas API is shutting down. Goodbye! \\o");
    }

    @Nullable
    private String readBanner() {
        try (InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream("banner.txt")) {
            if (inputStream == null) {
                return null;
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final IOException e) {
            LOG.warn("Failed to read banner.txt", e);
            return null;
        }
    }
}
