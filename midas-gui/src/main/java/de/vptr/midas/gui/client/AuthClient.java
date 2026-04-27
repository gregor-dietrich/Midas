package de.vptr.midas.gui.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

/** Component. */
@RegisterRestClient(configKey = "midas-api")
@Path("/auth")
public interface AuthClient {

    @HEAD
    Response validateCredentials(@HeaderParam("Authorization") String authorization);
}
