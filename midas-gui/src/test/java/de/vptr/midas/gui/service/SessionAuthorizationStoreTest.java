package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class SessionAuthorizationStoreTest {

    @Test
    void createSession_shouldStoreAndResolveAuthorization() {
        final var sessionAuthorizationStore = new SessionAuthorizationStore();
        final var sessionId = sessionAuthorizationStore.createSession("Basic abc");

        assertThat(sessionId).isNotBlank();
        assertThat(sessionAuthorizationStore.resolveAuthorization(sessionId)).isEqualTo("Basic abc");
    }

    @Test
    void revokeSession_shouldRemoveStoredAuthorization() {
        final var sessionAuthorizationStore = new SessionAuthorizationStore();
        final var sessionId = sessionAuthorizationStore.createSession("Basic def");

        sessionAuthorizationStore.revokeSession(sessionId);

        assertThat(sessionAuthorizationStore.resolveAuthorization(sessionId)).isNull();
    }

    @Test
    void resolveAndRevoke_shouldHandleNullSessionId() {
        final var sessionAuthorizationStore = new SessionAuthorizationStore();
        assertThat(sessionAuthorizationStore.resolveAuthorization(null)).isNull();

        sessionAuthorizationStore.revokeSession(null);
    }

    @Test
    void resolveAuthorization_shouldExpireInactiveSessions() {
        final var clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        final var sessionAuthorizationStore = new SessionAuthorizationStore(clock);
        final var sessionId = sessionAuthorizationStore.createSession("Basic ghi");

        clock.advance(SessionAuthorizationStore.SESSION_TTL.plusSeconds(1));

        assertThat(sessionAuthorizationStore.resolveAuthorization(sessionId)).isNull();
        assertThat(sessionAuthorizationStore.storedSessionCount()).isZero();
    }

    @Test
    void resolveAuthorization_shouldExpireSessionAtExactTtlBoundary() {
        final var clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        final var sessionAuthorizationStore = new SessionAuthorizationStore(clock);
        final var sessionId = sessionAuthorizationStore.createSession("Basic exact");

        clock.advance(SessionAuthorizationStore.SESSION_TTL);

        assertThat(sessionAuthorizationStore.resolveAuthorization(sessionId)).isNull();
        assertThat(sessionAuthorizationStore.storedSessionCount()).isZero();
    }

    @Test
    void resolveAuthorization_shouldRefreshExpiryForActiveSessions() {
        final var clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        final var sessionAuthorizationStore = new SessionAuthorizationStore(clock);
        final var sessionId = sessionAuthorizationStore.createSession("Basic jkl");

        clock.advance(SessionAuthorizationStore.SESSION_TTL.minusSeconds(1));
        assertThat(sessionAuthorizationStore.resolveAuthorization(sessionId)).isEqualTo("Basic jkl");

        clock.advance(SessionAuthorizationStore.SESSION_TTL.minusSeconds(1));
        assertThat(sessionAuthorizationStore.resolveAuthorization(sessionId)).isEqualTo("Basic jkl");
    }

    @Test
    void cleanupExpiredSessions_shouldRemoveOnlyExpiredEntries() {
        final var clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        final var sessionAuthorizationStore = new SessionAuthorizationStore(clock);
        final var expiredSessionId = sessionAuthorizationStore.createSession("Basic mno");

        clock.advance(SessionAuthorizationStore.SESSION_TTL.plusSeconds(1));
        final var activeSessionId = sessionAuthorizationStore.createSession("Basic pqr");

        sessionAuthorizationStore.cleanupExpiredSessions();

        assertThat(sessionAuthorizationStore.resolveAuthorization(expiredSessionId)).isNull();
        assertThat(sessionAuthorizationStore.resolveAuthorization(activeSessionId)).isEqualTo("Basic pqr");
        assertThat(sessionAuthorizationStore.storedSessionCount()).isEqualTo(1);
    }

    private static final class MutableClock extends Clock {
        private Instant currentInstant;

        private MutableClock(final Instant currentInstant) {
            this.currentInstant = currentInstant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(final java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return this.currentInstant;
        }

        private void advance(final java.time.Duration duration) {
            this.currentInstant = this.currentInstant.plus(duration);
        }
    }
}
