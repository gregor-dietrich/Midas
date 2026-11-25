package de.vptr.midas.api.resource;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

/**
 * Liveness probe for the API service. Confirms the application is running and able to serve requests. The datasource
 * readiness check is auto-registered by Quarkus.
 */
@Liveness
@ApplicationScoped
public class LivenessHealthCheck implements HealthCheck {

    @Override
    public HealthCheckResponse call() {
        return HealthCheckResponse.named("midas-api").up().build();
    }
}
