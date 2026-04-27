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
 * Uses SmallRye Fault Tolerance for retry, timeout, and fallback.
 */
@ApplicationScoped
public class BackendAvailabilityService {

    private static final Logger LOG = Logger.getLogger(BackendAvailabilityService.class);

    @Inject
    @RestClient
    BackendHealthClient backendHealthClient;

    /** Returns whether the backend available. */
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    @Retry(maxRetries = 2, delay = 500, delayUnit = ChronoUnit.MILLIS)
    @Fallback(fallbackMethod = "fallbackAvailability")
    public boolean isBackendAvailable() {
        LOG.trace("Checking backend availability via /q/health");
        try (var response = this.backendHealthClient.checkHealth()) {
            final var available = response.getStatus() == 200;
            if (!available) {
                LOG.warnf("Backend returned non-200 status: %s", response.getStatus());
            }
            return available;
        } catch (final RuntimeException e) {
            LOG.warn("Backend availability check attempt failed", e);
            throw e;
        }
    }

    @SuppressWarnings("unused")
    private boolean fallbackAvailability() {
        LOG.error("Backend availability check failed after retries");
        return false;
    }
}
