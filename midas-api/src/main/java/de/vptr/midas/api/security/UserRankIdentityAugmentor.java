package de.vptr.midas.api.security;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.eclipse.microprofile.context.ManagedExecutor;

import de.vptr.midas.api.entity.UserEntity;
import de.vptr.midas.api.entity.UserRankEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Security identity augmentor that adds roles based on user rank. */
@ApplicationScoped
public class UserRankIdentityAugmentor implements SecurityIdentityAugmentor {

    private static final Map<String, Function<UserRankEntity, Boolean>> PERMISSION_MAP = Map.ofEntries(
            Map.entry("admin:view", r -> r.adminView), Map.entry("page:add", r -> r.pageAdd),
            Map.entry("page:delete", r -> r.pageDelete), Map.entry("page:edit", r -> r.pageEdit),
            Map.entry("post:add", r -> r.postAdd), Map.entry("post:delete", r -> r.postDelete),
            Map.entry("post:edit", r -> r.postEdit), Map.entry("post-category:add", r -> r.postCategoryAdd),
            Map.entry("post-category:delete", r -> r.postCategoryDelete),
            Map.entry("post-category:edit", r -> r.postCategoryEdit),
            Map.entry("post-comment:add", r -> r.postCommentAdd),
            Map.entry("post-comment:delete", r -> r.postCommentDelete),
            Map.entry("post-comment:edit", r -> r.postCommentEdit), Map.entry("user:add", r -> r.userAdd),
            Map.entry("user:delete", r -> r.userDelete), Map.entry("user:edit", r -> r.userEdit),
            Map.entry("user-group:add", r -> r.userGroupAdd), Map.entry("user-group:delete", r -> r.userGroupDelete),
            Map.entry("user-group:edit", r -> r.userGroupEdit), Map.entry("user-account:add", r -> r.userAccountAdd),
            Map.entry("user-account:delete", r -> r.userAccountDelete),
            Map.entry("user-account:edit", r -> r.userAccountEdit), Map.entry("user-rank:add", r -> r.userRankAdd),
            Map.entry("user-rank:delete", r -> r.userRankDelete), Map.entry("user-rank:edit", r -> r.userRankEdit));

    @Inject
    ManagedExecutor executor;

    @Override
    public Uni<SecurityIdentity> augment(final SecurityIdentity identity, final AuthenticationRequestContext context) {
        if (identity.isAnonymous()) {
            return Uni.createFrom().item(identity);
        }

        final var username = identity.getPrincipal().getName();

        return Uni.createFrom().item(() -> this.augmentIdentity(identity, username)).runSubscriptionOn(this.executor);
    }

    SecurityIdentity augmentIdentity(final SecurityIdentity identity, final String username) {
        return QuarkusTransaction.joiningExisting().call(() -> {
            final Optional<UserEntity> userOpt = UserEntity.find("username", username).firstResultOptional();
            if (userOpt.isEmpty()) {
                return identity;
            }
            final UserEntity user = userOpt.get();
            final UserRankEntity rank = user.rank;
            if (rank == null) {
                return identity;
            }

            final Set<String> roles = this.buildRolesFromUserRank(rank);

            return QuarkusSecurityIdentity.builder(identity).addRoles(roles).build();
        });
    }

    /**
     * Builds the set of role strings from the given user rank's permission flags.
     */
    Set<String> buildRolesFromUserRank(final UserRankEntity rank) {
        final Set<String> roles = new HashSet<>();
        PERMISSION_MAP.forEach((role, getter) -> {
            if (Boolean.TRUE.equals(getter.apply(rank))) {
                roles.add(role);
            }
        });
        return roles;
    }
}
