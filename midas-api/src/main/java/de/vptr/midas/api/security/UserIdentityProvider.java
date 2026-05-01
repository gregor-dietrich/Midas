package de.vptr.midas.api.security;

import java.time.LocalDateTime;
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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
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

        return Uni.createFrom()
                .item(() -> this.authenticateUser(username, password, clientKey))
                .runSubscriptionOn(this.executor);
    }

    @Transactional
    SecurityIdentity authenticateUser(final String username, final char[] password, final String clientKey) {
        try {
            if (this.authFailureRateLimitService.isBlocked(clientKey)) {
                throw new AuthenticationFailedException("Too many invalid authentication attempts");
            }

            final UserEntity user = UserEntity.find("username = ?1", username).firstResult();

            if (user == null) {
                // Run dummy hash to prevent timing-based username enumeration
                this.passwordHashingService.verifyPasswordChars(password, "dummy", "dummySalt");
                this.authFailureRateLimitService.recordFailure(clientKey);
                throw new AuthenticationFailedException("Invalid credentials");
            }

            if (!this.passwordHashingService.verifyPasswordChars(password, user.password, user.salt)) {
                this.authFailureRateLimitService.recordFailure(clientKey);
                throw new AuthenticationFailedException("Invalid credentials");
            }

            if (user.banned) {
                this.authFailureRateLimitService.recordFailure(clientKey);
                throw new AuthenticationFailedException("User is banned");
            }

            if (!user.activated) {
                this.authFailureRateLimitService.recordFailure(clientKey);
                throw new AuthenticationFailedException("User is not activated");
            }

            // Update lastLogin - ignore optimistic lock exceptions as they're not critical
            // for authentication
            try {
                this.entityManager.createQuery("UPDATE UserEntity u SET u.lastLogin = :now WHERE u.id = :id")
                        .setParameter("now", LocalDateTime.now())
                        .setParameter("id", user.id)
                        .executeUpdate();
            } catch (final RuntimeException e) {
                // Log but don't fail authentication for last_login update issues
                // (OptimisticLockException, etc.)
                LOG.debugf("Failed to update last_login for user %s (this is expected during concurrent logins): %s",
                        user.username, e.getMessage());
            }

            return QuarkusSecurityIdentity.builder()
                    .setPrincipal(new QuarkusPrincipal(username))
                    .build();
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
        return authenticateUser(username, password, username);
    }

    private static void clearPassword(final char[] password) {
        if (password != null) {
            Arrays.fill(password, '\0');
        }
    }
}
