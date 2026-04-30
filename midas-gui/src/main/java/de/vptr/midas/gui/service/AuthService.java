package de.vptr.midas.gui.service;

import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import com.vaadin.flow.server.VaadinSession;

import de.vptr.midas.gui.client.AuthClient;
import de.vptr.midas.gui.dto.AuthResultDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;

/** Component. */
@ApplicationScoped
public class AuthService {
    private static final Logger LOG = Logger.getLogger(AuthService.class);
    private static final SessionAttribute<String> USERNAME_ATTRIBUTE = SessionAttribute.of("authenticated.username",
            String.class);
    private static final SessionAttribute<String> SESSION_ID_ATTRIBUTE = SessionAttribute.of("authenticated.session-id",
            String.class);
    private static final SessionAttribute<Boolean> AUTHENTICATED_ATTRIBUTE = SessionAttribute.of("authenticated.status",
            Boolean.class);

    @Inject
    @RestClient
    AuthClient authClient;

    @Inject
    UserRankService userRankService;

    @Inject
    SessionAuthorizationStore sessionAuthorizationStore;

    /** Authenticates the user credentials. */
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    public AuthResultDto authenticate(final String username, final String password) {
        LOG.trace("Starting authentication");

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            LOG.trace("Username or password is empty");
            return AuthResultDto.invalidInput();
        }

        try {
            final var authHeader = "Basic "
                    + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

            this.authClient.validateCredentials(authHeader);

            final var session = VaadinSession.getCurrent();
            if (session == null) {
                LOG.warn("No VaadinSession available after successful credential validation");
                return AuthResultDto.backendUnavailable();
            }
            final var previousSessionId = SESSION_ID_ATTRIBUTE.read(session, LOG).orElse(null);
            this.sessionAuthorizationStore.revokeSession(previousSessionId);
            final var sessionId = this.sessionAuthorizationStore.createSession(authHeader);

            USERNAME_ATTRIBUTE.write(session, username);
            SESSION_ID_ATTRIBUTE.write(session, sessionId);
            AUTHENTICATED_ATTRIBUTE.write(session, Boolean.TRUE);

            LOG.trace("User authenticated successfully");
            return AuthResultDto.success();

        } catch (final WebApplicationException e) {
            if (e.getResponse().getStatus() == 401) {
                LOG.trace("Authentication failed - invalid credentials");
                return AuthResultDto.invalidCredentials();
            } else {
                LOG.errorf("Backend returned HTTP error %s during authentication", e.getResponse().getStatus());
                return AuthResultDto.backendUnavailable();
            }
        } catch (final ProcessingException e) {
            LOG.error("Backend connection failed during authentication", e);
            return AuthResultDto.backendUnavailable();
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error during authentication", e);
            return AuthResultDto.backendUnavailable();
        }
    }

    /** Logs out the current user. */
    public void logout() {
        LOG.trace("Logging out current user");

        // Clear cached user rank before closing the session
        this.userRankService.clearCachedUserRank();

        final var session = VaadinSession.getCurrent();
        if (session != null) {
            final var sessionId = SESSION_ID_ATTRIBUTE.read(session, LOG).orElse(null);
            this.sessionAuthorizationStore.revokeSession(sessionId);
            this.clearAuthenticationState(session);
            session.close();
        }

        LOG.trace("User logged out");
    }

    /** Returns whether the authenticated. */
    public boolean isAuthenticated() {
        final var session = VaadinSession.getCurrent();
        if (session == null) {
            LOG.trace("No VaadinSession available, returning not authenticated");
            return false;
        }
        final var authenticated = AUTHENTICATED_ATTRIBUTE.read(session, LOG).orElse(Boolean.FALSE);
        if (!authenticated) {
            LOG.trace("Checking authentication status: false");
            return false;
        }
        final var sessionId = SESSION_ID_ATTRIBUTE.read(session, LOG).orElse(null);
        if (sessionId == null) {
            LOG.warn("Session marked authenticated but has no session id");
            this.clearAuthenticationState(session);
            return false;
        }
        final var hasAuthorization = this.sessionAuthorizationStore.resolveAuthorization(sessionId) != null;
        if (!hasAuthorization) {
            LOG.warn("Session marked authenticated but has no authorization mapping");
            this.clearAuthenticationState(session);
        }
        final var result = hasAuthorization;
        LOG.tracef("Checking authentication status: %s", result);
        return result;
    }

    /** Returns the authorization header for the current authenticated session. */
    public String getAuthorizationHeader() {
        if (!this.isAuthenticated()) {
            LOG.warn("Attempting to get auth header but user is not authenticated");
            return null;
        }

        final var session = VaadinSession.getCurrent();
        if (session == null) {
            LOG.warn("No VaadinSession available, cannot read authorization");
            return null;
        }
        final var sessionId = SESSION_ID_ATTRIBUTE.read(session, LOG).orElse(null);
        if (sessionId == null) {
            LOG.warn("No session id available for authenticated session");
            this.clearAuthenticationState(session);
            return null;
        }
        final var header = this.sessionAuthorizationStore.resolveAuthorization(sessionId);
        if (header == null) {
            LOG.warn("Authorization mapping missing for current session");
            this.clearAuthenticationState(session);
            return null;
        }

        return header;
    }

    /**
     * Returns the authorization header or throws when the session is not
     * authenticated.
     */
    public String requireAuthorizationHeader() {
        final var authHeader = this.getAuthorizationHeader();
        if (authHeader == null) {
            throw new AuthenticationException("Authentication required");
        }
        return authHeader;
    }

    /**
     * Returns the authorization header for backward compatibility with existing
     * callers.
     *
     * @deprecated Use {@link #getAuthorizationHeader()} instead.
     */
    @Deprecated
    public String getBasicAuthHeader() {
        return this.getAuthorizationHeader();
    }

    /**
     * Returns the username of the currently authenticated user, or null if not
     * authenticated.
     */
    public String getUsername() {
        final var session = VaadinSession.getCurrent();
        return USERNAME_ATTRIBUTE.read(session, LOG).orElse(null);
    }

    private void clearAuthenticationState(final VaadinSession session) {
        USERNAME_ATTRIBUTE.clear(session);
        SESSION_ID_ATTRIBUTE.clear(session);
        AUTHENTICATED_ATTRIBUTE.write(session, Boolean.FALSE);
    }
}
