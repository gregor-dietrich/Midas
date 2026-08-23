package de.vptr.midas.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

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

    @Inject
    LaunchMode launchMode;

    @ConfigProperty(name = "quarkus.datasource.password", defaultValue = "")
    String dbPassword = "";

    void onStart(@Observes final StartupEvent ev) {
        this.failOnDefaultDbPasswordInProduction();

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
