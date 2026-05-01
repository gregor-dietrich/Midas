package de.vptr.midas.gui.service;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import de.vptr.midas.gui.client.BackendHealthClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;

/**
 * Readiness probe that verifies the backend API is reachable.
 * Reports UP when the API's /q/health endpoint returns successfully.
 */
@Readiness
@ApplicationScoped
public class BackendHealthCheck implements HealthCheck {

    private static final Logger LOG = Logger.getLogger(BackendHealthCheck.class);

    @Inject
    @RestClient
    BackendHealthClient backendHealthClient;

    @Override
    public HealthCheckResponse call() {
        try (var response = this.backendHealthClient.checkHealth()) {
            final var status = response.getStatus();
            if (status == 200) {
                LOG.trace("Backend health check passed");
                return HealthCheckResponse.named("midas-api-backend").up().build();
            }
            LOG.warnf("Backend health check returned status: %s", status);
            return HealthCheckResponse.named("midas-api-backend")
                    .down()
                    .withData("status", status)
                    .build();
        } catch (final ProcessingException e) {
            LOG.warnf("Backend health check failed (connection error): %s", e.getMessage());
            return HealthCheckResponse.named("midas-api-backend")
                    .down()
                    .withData("error", e.getMessage())
                    .build();
        } catch (final WebApplicationException e) {
            LOG.warnf("Backend health check failed (HTTP error): %s", e.getMessage());
            return HealthCheckResponse.named("midas-api-backend")
                    .down()
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}
