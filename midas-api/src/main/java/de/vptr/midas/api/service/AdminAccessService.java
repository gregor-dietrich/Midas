package de.vptr.midas.api.service;

import de.vptr.midas.api.entity.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;

/** Shared admin permission checks for service-layer authorization. */
@ApplicationScoped
public class AdminAccessService {

    /** Returns true when user exists and has admin view permission. */
    // TODO P-11: Cache this result per request to avoid repeated DB round-trips on every
    // ownership verification. Consider using a @RequestScoped cache bean.
    public boolean hasAdminViewAccess(final String username) {
        return UserEntity.<UserEntity>find("username", username).firstResultOptional()
                .map(user -> user.rank != null && Boolean.TRUE.equals(user.rank.adminView))
                .orElse(false);
    }
}
