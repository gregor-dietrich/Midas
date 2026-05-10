package de.vptr.midas.gui.service;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import de.vptr.midas.gui.client.AuthClient;
import de.vptr.midas.gui.exception.AuthenticationException;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

/** Component. */
@Dependent
public class GreetService {

    private static final Logger LOG = Logger.getLogger(GreetService.class);

    @Inject
    @RestClient
    AuthClient authClient;

    @Inject
    AuthService authService;

    /**
     * Returns a greeting for the given name.
     *
     * @throws AuthenticationException
     *             if the user is not authenticated
     */
    public String greet(final String name) {
        LOG.debugf("GreetService.greet called with name: %s", name);

        final var authHeader = this.authService.getAuthorizationHeader();
        if (authHeader == null) {
            throw new AuthenticationException("Not authenticated");
        }

        var greeting = "Hello, ";
        if (name == null || name.isEmpty()) {
            greeting += "anonymous user";
        } else {
            greeting += name;
        }

        return greeting + "!";
    }
}
