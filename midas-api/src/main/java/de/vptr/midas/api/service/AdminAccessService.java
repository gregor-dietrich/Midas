package de.vptr.midas.api.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.vptr.midas.api.entity.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;

/** Shared admin permission checks for service-layer authorization. */
@ApplicationScoped
public class AdminAccessService {

    private final ThreadLocal<Map<String, Boolean>> requestCache = ThreadLocal.withInitial(ConcurrentHashMap::new);

    /** Returns true when user exists and has admin view permission. Caches result per request. */
    public boolean hasAdminViewAccess(final String username) {
        if (username == null) {
            return false;
        }
        final var cache = this.requestCache.get();
        return cache.computeIfAbsent(username, key ->
                UserEntity.<UserEntity>find("username", key).firstResultOptional()
                        .map(user -> user.rank != null && Boolean.TRUE.equals(user.rank.adminView))
                        .orElse(false)
        );
    }

    /** Clears the per-request cache. Call at end of request or transaction. */
    public void clearCache() {
        this.requestCache.remove();
    }
}
