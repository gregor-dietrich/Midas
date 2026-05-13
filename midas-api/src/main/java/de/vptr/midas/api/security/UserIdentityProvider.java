package de.vptr.midas.api.security;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

import de.vptr.midas.api.entity.UserEntity;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;

/** Type definition. */
@ApplicationScoped
public class UserIdentityProvider implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

    private static final Logger LOG = Logger.getLogger(UserIdentityProvider.class);

    @Inject
    ManagedExecutor executor;

    @Inject
    EntityManager entityManager;

    @Inject
    PasswordHashingService passwordHashingService;

    @Inject
    AuthFailureRateLimitService authFailureRateLimitService;

    @Inject
    LoginAttemptService loginAttemptService;

    @Inject
    jakarta.enterprise.inject.Instance<io.vertx.core.http.HttpServerRequest> httpServerRequestInstance;

    @Override
    public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
        return UsernamePasswordAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(final UsernamePasswordAuthenticationRequest request,
            final AuthenticationRequestContext context) {

        final String username = request.getUsername();
        final char[] password = request.getPassword() != null ? request.getPassword().getPassword().clone() : null;
        final String clientKey = username;

        return Uni.createFrom().item(() -> this.authenticateUser(username, password, clientKey))
                .runSubscriptionOn(this.executor);
    }

    @Transactional
    SecurityIdentity authenticateUser(final String username, @Nullable final char[] password, final String clientKey) {
        String ip = "unknown";
        try {
            if (this.httpServerRequestInstance.isResolvable()) {
                ip = this.httpServerRequestInstance.get().remoteAddress().host();
            }
        } catch (final Exception e) {
            LOG.debug("Could not determine client IP, using 'unknown'");
        }
        try {
            if (this.authFailureRateLimitService.isBlocked(clientKey)) {
                throw new AuthenticationFailedException("Too many invalid authentication attempts");
            }

            final long delay = this.loginAttemptService.getDelay(username, ip);
            if (delay > 0) {
                try {
                    Thread.sleep(Math.min(delay * 1000, 30000)); // Sleep with cap
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            final UserEntity user = UserEntity.find("username = ?1", username).firstResult();

            if (user == null) {
                // Run dummy hash to prevent timing-based username enumeration
                this.passwordHashingService.verifyPassword("dummy",
                        "$2a$10$vptr.midas.dummy.hash.for.enumeration.protection.ZZZ");
                this.authFailureRateLimitService.recordFailure(clientKey);
                this.loginAttemptService.recordFailure(username, ip);
                throw new AuthenticationFailedException("Invalid credentials");
            }

            final String passwordStr = password != null ? new String(password) : "";

            if (user.password == null
                    || !this.passwordHashingService.verifyPassword(passwordStr, user.password)) {
                this.authFailureRateLimitService.recordFailure(clientKey);
                this.loginAttemptService.recordFailure(username, ip);
                throw new AuthenticationFailedException("Invalid credentials");
            }

            if (user.banned) {
                this.authFailureRateLimitService.recordFailure(clientKey);
                this.loginAttemptService.recordFailure(username, ip);
                throw new AuthenticationFailedException("User is banned");
            }

            if (!user.activated) {
                this.authFailureRateLimitService.recordFailure(clientKey);
                this.loginAttemptService.recordFailure(username, ip);
                throw new AuthenticationFailedException("User is not activated");
            }

            this.loginAttemptService.recordSuccess(username, ip);

            // Update lastLogin - ignore optimistic lock exceptions as they're not critical
            // for authentication
            try {
                this.entityManager.createQuery("UPDATE UserEntity u SET u.lastLogin = :now WHERE u.id = :id")
                        .setParameter("now", LocalDateTime.now(ZoneOffset.UTC)).setParameter("id", user.id)
                        .executeUpdate();
            } catch (final OptimisticLockException e) {
                LOG.debugf("Failed to update last_login for user %s (concurrent login): %s", user.id, e.getMessage());
            }

            return QuarkusSecurityIdentity.builder().setPrincipal(new QuarkusPrincipal(username)).build();
        } finally {
            clearPassword(password);
        }
    }

    @Transactional
    SecurityIdentity authenticateUser(final String username, final String password, final String clientKey) {
        final char[] passwordChars = password != null ? password.toCharArray() : null;
        try {
            return this.authenticateUser(username, passwordChars, clientKey);
        } finally {
            clearPassword(passwordChars);
        }
    }

    @Transactional
    SecurityIdentity authenticateUser(final String username, final String password) {
        return this.authenticateUser(username, password, username);
    }

    private static void clearPassword(@Nullable final char[] password) {
        if (password != null) {
            Arrays.fill(password, '\0');
        }
    }
}
