package de.vptr.midas.gui.service;

import java.time.temporal.ChronoUnit;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import de.vptr.midas.gui.client.BackendHealthClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Service for checking backend availability during navigation.
 *
 * <p>
 * Uses SmallRye Fault Tolerance (retry, fallback) for resilience. REST clients are eagerly initialized at startup (see
 * RestClientEagerInitializer) so decorators work correctly even in dev mode.
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
     * <p>
     * Retry once on transient failures. Falls back to false if all retries exhaust.
     *
     * <p>
     * REST clients are pre-initialized at startup, so this works from any thread.
     */
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

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "Called reflectively by SmallRye Fault Tolerance @Fallback")
    private boolean fallbackAvailability() {
        LOG.error("Backend availability check failed after retries, assuming unavailable");
        return false;
    }

    /**
     * Raw health check without SmallRye Fault Tolerance decorators.
     *
     * <p>
     * Safe for use from non-Quarkus threads (e.g. ForkJoinPool) where timeout and retry interceptors fail due to
     * missing config context.
     */
    public boolean isBackendAvailableRaw() {
        LOG.trace("Checking backend availability (raw)");
        try (var response = this.backendHealthClient.checkHealth()) {
            final var available = response.getStatus() == 200;
            if (!available) {
                LOG.warnf("Backend returned non-200 status: %s", response.getStatus());
            }
            return available;
        } catch (final RuntimeException e) {
            LOG.debugf("Raw health check failed: %s", e.getMessage());
            return false;
        }
    }
}
