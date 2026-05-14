package de.vptr.midas.api.security;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

import de.vptr.midas.api.entity.UserEntity;
import de.vptr.midas.api.util.IpUtil;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
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
    Instance<RoutingContext> routingContextInstance;

    @ConfigProperty(name = "rate-limit.trust-forwarded-for", defaultValue = "false")
    boolean trustForwardedFor;

    @Override
    public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
        return UsernamePasswordAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(final UsernamePasswordAuthenticationRequest request,
            final AuthenticationRequestContext context) {

        final var username = request.getUsername();
        final var password = request.getPassword() != null ? request.getPassword().getPassword().clone() : null;

        var ip = "unknown";
        if (Vertx.currentContext() != null) {
            ip = Vertx.currentContext().getLocal("midas.client-ip");
        }

        if (ip == null || "unknown".equals(ip)) {
            try {
                if (this.routingContextInstance.isResolvable()) {
                    ip = IpUtil.resolveIp(this.routingContextInstance.get().request(), this.trustForwardedFor);
                }
            } catch (final RuntimeException e) {
                LOG.debug("Could not determine client IP via CDI, using 'unknown'");
            }
        }

        final var finalIp = ip != null ? ip : "unknown";
        return Uni.createFrom().item(() -> this.authenticateUser(username, password, finalIp))
                .runSubscriptionOn(this.executor);
    }

    @Transactional
    SecurityIdentity authenticateUser(final String username, @Nullable final char[] password, final String ip) {
        final var clientKey = username + ":" + ip;
        try {
            if (this.authFailureRateLimitService.isBlocked(clientKey)) {
                throw new AuthenticationFailedException("Too many invalid authentication attempts");
            }

            final var delay = this.loginAttemptService.getDelay(username, ip);
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

            if (user.password == null || !this.passwordHashingService.verifyPassword(passwordStr, user.password)) {
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
    SecurityIdentity authenticateUser(final String username, final String password) {
        final char[] passwordChars = password != null ? password.toCharArray() : null;
        try {
            return this.authenticateUser(username, passwordChars, "unknown");
        } finally {
            clearPassword(passwordChars);
        }
    }

    private static void clearPassword(@Nullable final char[] password) {
        if (password == null) {
            return;
        }
        Arrays.fill(password, '\0');
    }
}
