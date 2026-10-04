package de.vptr.midas.gui.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST client for the backend API health endpoint. Has its own config key and no fault-tolerance retry, so a probe
 * gives up after its own read timeout instead of retrying like the generated API clients.
 */
@RegisterRestClient(configKey = "midas-api-health")
@Path("/q/health")
@FunctionalInterface
public interface BackendHealthClient {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    Response checkHealth();
}
