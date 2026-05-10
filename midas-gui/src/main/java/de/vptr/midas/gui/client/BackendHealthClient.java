package de.vptr.midas.gui.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST client for the backend API health endpoint. Uses a separate config key to target the API root (not /api/v1).
 */
@RegisterRestClient(configKey = "midas-api-health")
@Path("/q/health")
public interface BackendHealthClient {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Response checkHealth();
}
