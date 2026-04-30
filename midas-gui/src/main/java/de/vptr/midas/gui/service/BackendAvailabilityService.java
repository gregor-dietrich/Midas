package de.vptr.midas.gui.service;

import java.time.temporal.ChronoUnit;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import de.vptr.midas.gui.client.BackendHealthClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service for checking backend availability during navigation.
 *
 * <p>Uses SmallRye Fault Tolerance (retry, timeout, fallback) for resilience.
 * REST clients are eagerly initialized at startup (see RestClientEagerInitializer)
 * so decorators work correctly even in dev mode.
 */
@ApplicationScoped
public class BackendAvailabilityService {

    private static final Logger LOG = Logger.getLogger(BackendAvailabilityService.class);

    @Inject
    @RestClient
    BackendHealthClient backendHealthClient;

    /**
     * Returns whether the backend is available.
     *
     * <p>Timeout is 7s (REST client's connect + read timeouts = 5s max, plus buffer).
     * Retry once on transient failures. Falls back to false if all retries exhaust.
     *
     * <p>REST clients are pre-initialized at startup, so this works from any thread.
     */
    @Timeout(value = 7, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 1, delay = 200, delayUnit = ChronoUnit.MILLIS)
    @Fallback(fallbackMethod = "fallbackAvailability")
    public boolean isBackendAvailable() {
        LOG.trace("Checking backend availability");
        try (var response = this.backendHealthClient.checkHealth()) {
            final var available = response.getStatus() == 200;
            if (!available) {
                LOG.warnf("Backend returned non-200 status: %s", response.getStatus());
            }
            return available;
        } catch (final RuntimeException e) {
            LOG.tracef("Health check failed (will retry): %s", e.getMessage());
            throw e;
        }
    }

    @SuppressWarnings("unused")
    private boolean fallbackAvailability() {
        LOG.error("Backend availability check failed after retries, assuming unavailable");
        return false;
    }
}
