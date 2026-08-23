package de.vptr.midas.api.security;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

import de.vptr.midas.api.entity.UserEntity;
import de.vptr.midas.api.util.IpUtil;
import io.quarkus.elytron.security.common.BcryptUtil;
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
import jakarta.annotation.PostConstruct;
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

    /**
     * A real, valid bcrypt hash used as a constant-time decoy when the user does not exist or is disabled. It is
     * computed once at class load so that {@link PasswordHashingService#verifyPassword} actually runs the bcrypt KDF on
     * the decoy path, equalising response time with the genuine wrong-password path and preventing timing-based
     * username enumeration. (A malformed hash would make verifyPassword fail fast and defeat the protection.)
     */
    private static final String DUMMY_HASH = BcryptUtil.bcryptHash("midas-timing-protection-decoy");

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

    @ConfigProperty(name = "app.security.trusted-proxy-ips", defaultValue = "127.0.0.1,::1,0:0:0:0:0:0:0:1")
    @Nullable
    String trustedProxyIpsConfig;

    private Set<String> trustedProxyIps;

    @PostConstruct
    void initConfig() {
        this.trustedProxyIps = IpUtil.parseTrustedProxyIps(this.trustedProxyIpsConfig);
    }

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
                    ip = IpUtil.resolveIp(this.routingContextInstance.get().request(), this.trustedProxyIps);
                }
            } catch (final RuntimeException e) {
                LOG.debug("Could not determine client IP via CDI, using 'unknown'");
            }
        }

        final var finalIp = ip != null ? ip : "unknown";
        return Uni.createFrom().item(() -> this.authenticateUser(username, password, finalIp))
                .runSubscriptionOn(this.executor);
    }

    SecurityIdentity authenticateUser(final String username, @Nullable final char[] password, final String ip) {
        final var clientKey = username + ":" + ip;
        try {
            if (this.authFailureRateLimitService.isBlocked(clientKey)) {
                throw new AuthenticationFailedException("Too many invalid authentication attempts");
            }

            final var delay = this.loginAttemptService.getDelay(username, ip);
            if (delay > 0) {
                try {
                    // Sleep before opening a DB transaction to avoid holding a pooled connection
                    Thread.sleep(Math.min(delay * 1000, 30_000));
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            return this.performAuthentication(username, password, ip, clientKey);
        } finally {
            clearPassword(password);
        }
    }

    SecurityIdentity authenticateUser(final String username, final String password) {
        final char[] passwordChars = password != null ? password.toCharArray() : null;
        try {
            return this.authenticateUser(username, passwordChars, "unknown");
        } finally {
            clearPassword(passwordChars);
        }
    }

    @Transactional
    SecurityIdentity performAuthentication(final String username, @Nullable final char[] password, final String ip,
            final String clientKey) {
        final UserEntity user = UserEntity.find("username = ?1", username).firstResult();

        if (user == null || user.banned || !user.activated || user.password == null) {
            // Run a real bcrypt verification against a decoy hash to prevent timing-based username enumeration
            this.passwordHashingService.verifyPassword("dummy", DUMMY_HASH);
            this.authFailureRateLimitService.recordFailure(clientKey);
            this.loginAttemptService.recordFailure(username, ip);

            if (user == null || user.password == null) {
                throw new AuthenticationFailedException("Invalid credentials");
            } else if (user.banned) {
                throw new AuthenticationFailedException("User is banned");
            } else {
                throw new AuthenticationFailedException("User is not activated");
            }
        }

        final String passwordStr = password != null ? new String(password) : "";

        if (!this.passwordHashingService.verifyPassword(passwordStr, user.password)) {
            this.authFailureRateLimitService.recordFailure(clientKey);
            this.loginAttemptService.recordFailure(username, ip);
            throw new AuthenticationFailedException("Invalid credentials");
        }

        this.loginAttemptService.recordSuccess(username, ip);

        // Update lastLogin - ignore optimistic lock exceptions as they're not critical
        // for authentication
        try {
            this.entityManager.createQuery("UPDATE UserEntity u SET u.lastLogin = :now WHERE u.id = :id")
                    .setParameter("now", LocalDateTime.now(ZoneOffset.UTC)).setParameter("id", user.id).executeUpdate();
        } catch (final OptimisticLockException e) {
            LOG.debugf("Failed to update last_login for user %s (concurrent login): %s", user.id, e.getMessage());
        }

        return QuarkusSecurityIdentity.builder().setPrincipal(new QuarkusPrincipal(username)).build();
    }

    private static void clearPassword(@Nullable final char[] password) {
        if (password == null) {
            return;
        }
        Arrays.fill(password, '\0');
    }
}
