package de.vptr.midas.gui.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

/** Stores authorization headers behind opaque session ids. */
@ApplicationScoped
public class SessionAuthorizationStore {

    static final Duration SESSION_TTL = Duration.ofMinutes(30);

    private static final int CLEANUP_INTERVAL = 64;
    private static final int MAX_SESSIONS = 10_000;

    private final Clock clock;
    private final AtomicInteger operationCount;
    private final Map<String, AuthorizationEntry> authorizationsBySessionId;

    public SessionAuthorizationStore() {
        this(Clock.systemUTC());
    }

    SessionAuthorizationStore(final Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.operationCount = new AtomicInteger();
        this.authorizationsBySessionId = new ConcurrentHashMap<>();
    }

    /**
     * Creates a new session id and stores the authorization header behind it.
     *
     * @param authorizationHeader the backend authorization header
     * @return generated opaque session id
     * @throws IllegalStateException if the session store is full
     */
    public String createSession(final String authorizationHeader) {
        this.cleanupExpiredSessionsIfNeeded();
        if (this.authorizationsBySessionId.size() >= MAX_SESSIONS) {
            this.cleanupExpiredSessions();
            if (this.authorizationsBySessionId.size() >= MAX_SESSIONS) {
                throw new IllegalStateException("Session store is full (max " + MAX_SESSIONS + " sessions)");
            }
        }
        final String sessionId = UUID.randomUUID().toString();
        this.authorizationsBySessionId.put(sessionId,
                new AuthorizationEntry(authorizationHeader, this.clock.instant().plus(SESSION_TTL)));
        return sessionId;
    }

    /**
     * Returns authorization header for the given session id.
     *
     * @param sessionId opaque session id
     * @return authorization header or null
     */
    public String resolveAuthorization(final String sessionId) {
        if (sessionId == null) {
            return null;
        }
        this.cleanupExpiredSessionsIfNeeded();

        final var now = this.clock.instant();
        final var entry = this.authorizationsBySessionId.computeIfPresent(sessionId, (key, existingEntry) -> {
            if (existingEntry.isExpired(now)) {
                return null;
            }
            return existingEntry.refresh(now.plus(SESSION_TTL));
        });
        return entry == null ? null : entry.authorizationHeader();
    }

    /**
     * Removes stored authorization for the given session id.
     *
     * @param sessionId opaque session id
     */
    public void revokeSession(final String sessionId) {
        if (sessionId != null) {
            this.authorizationsBySessionId.remove(sessionId);
        }
        this.cleanupExpiredSessionsIfNeeded();
    }

    void cleanupExpiredSessions() {
        final var now = this.clock.instant();
        this.authorizationsBySessionId.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    int storedSessionCount() {
        return this.authorizationsBySessionId.size();
    }

    private void cleanupExpiredSessionsIfNeeded() {
        if (this.operationCount.incrementAndGet() % CLEANUP_INTERVAL == 0) {
            this.cleanupExpiredSessions();
        }
    }

    private record AuthorizationEntry(String authorizationHeader, Instant expiresAt) {
        private boolean isExpired(final Instant now) {
            return !this.expiresAt.isAfter(now);
        }

        private AuthorizationEntry refresh(final Instant expiresAt) {
            return new AuthorizationEntry(this.authorizationHeader, expiresAt);
        }
    }
}
