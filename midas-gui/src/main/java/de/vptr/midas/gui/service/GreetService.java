package de.vptr.midas.gui.service;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import de.vptr.midas.gui.client.AuthClient;
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

    /** Handles the greet operation. */
    public String greet(final String name) {
        LOG.debugf("GreetService.greet called with name: %s", name);

        try {
            final var authHeader = this.authService.getAuthorizationHeader();
            if (authHeader == null) {
                return "Error: Not authenticated";
            }

            var greeting = "Hello, ";
            if (name == null || name.isEmpty()) {
                greeting += "anonymous user";
            } else {
                greeting += name;
            }

            return greeting + "!";
        } catch (final RuntimeException e) {
            LOG.error("Error calling greeting API", e);

            // Check if it's an authentication error
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                // Session might have expired, logout user
                this.authService.logout();
                return "Error: Session expired. Please log in again.";
            }

            return "Error: Could not connect to backend service";
        }
    }
}
