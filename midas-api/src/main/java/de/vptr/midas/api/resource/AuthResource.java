package de.vptr.midas.api.resource;

import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

import de.vptr.midas.api.util.ResponseUtil;
import io.quarkus.security.Authenticated;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

/** REST resource for authentication. */
@Path("/auth")
public class AuthResource {

    private static final Logger LOG = Logger.getLogger(AuthResource.class);

    @HEAD
    @Authenticated
    @APIResponse(responseCode = "200", description = "Credentials are valid", content = {})
    public Response validateAuth() {
        LOG.trace("validateCredentials() method called");
        return ResponseUtil.ok();
    }
}
