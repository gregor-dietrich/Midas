package de.vptr.midas.gui.client;

import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/** Component. */
@RegisterRestClient(configKey = "midas-api")
@Path("/api/v1/auth")
@FunctionalInterface
public interface AuthClient {

    @HEAD
    Response validateCredentials(@HeaderParam("Authorization") String authorization);
}
