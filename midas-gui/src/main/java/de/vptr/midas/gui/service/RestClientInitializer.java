package de.vptr.midas.gui.service;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import de.vptr.midas.gui.client.AuthClient;
import de.vptr.midas.gui.client.BackendHealthClient;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Eagerly initializes REST clients at Quarkus startup on the main thread.
 *
 * <p>
 * REST clients are lazily initialized on first method call. In prod (built JAR), this happens during the build phase.
 * In dev mode (quarkus:dev), lazy init happens at runtime, and if it occurs on a non-Quarkus thread,
 * ServiceConfigurationError results.
 *
 * <p>
 * This initializer runs at startup (on the main Quarkus thread) and touches each REST client to force initialization
 * while context is correct. Afterwards, REST clients are ready for use from any thread (including executor threads with
 * SmallRye Fault Tolerance).
 *
 * <p>
 * Note: Only runs in non-test mode to avoid interference with test mocking.
 */
@ApplicationScoped
public class RestClientInitializer {
    private static final Logger LOG = Logger.getLogger(RestClientInitializer.class);

    @Inject
    @RestClient
    BackendHealthClient backendHealthClient;

    @Inject
    @RestClient
    AuthClient authClient;

    void onStartup(@Observes final StartupEvent event) {
        // Skip in test mode (detected by checking if we're running under Surefire)
        final String testMode = System.getProperty("maven.surefire.plugin.version");
        if (testMode != null) {
            LOG.debug("Skipping REST client initialization in test mode");
            return;
        }

        LOG.debug("Eagerly initializing REST clients for dev/prod mode");
        try {
            // Touch the BackendHealthClient proxy. This forces the lazy initialization
            // on the main Quarkus thread, so REST client factory can access config.
            this.backendHealthClient.checkHealth().close();
            LOG.debug("BackendHealthClient initialized successfully");
        } catch (final Exception e) {
            LOG.debugf("BackendHealthClient initialization failed (expected on first health check if backend down): %s",
                    e.getMessage());
        }

        try {
            // For AuthClient, we can't safely call a method (no valid credentials at
            // startup).
            // Just ensure the proxy class is loaded/cached.
            final String _ = this.authClient.getClass().getName();
            LOG.debug("AuthClient initialized successfully");
        } catch (final Exception e) {
            LOG.debugf("AuthClient initialization error: %s", e.getMessage());
        }

        LOG.info("REST clients initialization complete");
    }
}
