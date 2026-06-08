package de.vptr.midas.api.security;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Service to track login attempts and implement exponential backoff.
 *
 * <p>
 * Entries carry a last-access timestamp and are evicted once idle past {@link #ENTRY_TTL}, bounding memory against
 * credential-spraying across many distinct username/IP keys.
 */
@ApplicationScoped
public class LoginAttemptService {

    /** Failures strictly below this threshold incur no delay. */
    private static final int BACKOFF_THRESHOLD = 5;

    /**
     * Upper bound on the backoff shift. Caps the delay at {@code 1 << 5 = 32s} and, crucially, keeps
     * {@code 1L << shift} far from overflow (the caller additionally clamps the actual sleep to 30s).
     */
    private static final int MAX_SHIFT = 5;

    /** Entries idle for longer than this are evicted to bound memory. */
    private static final Duration ENTRY_TTL = Duration.ofHours(1);

    /** Run the idle sweep once every this many operations. */
    private static final long CLEANUP_INTERVAL = 64;

    private final ConcurrentHashMap<String, Entry> attemptsCache = new ConcurrentHashMap<>();
    private final AtomicLong operationCount = new AtomicLong();

    /**
     * Records a failed login attempt for the given username and IP.
     *
     * @param username
     *            the username
     * @param ip
     *            the IP address
     */
    public void recordFailure(final String username, final String ip) {
        this.cleanupIfNeeded();
        this.attemptsCache.computeIfAbsent(this.buildKey(username, ip), ignored -> new Entry()).recordFailure();
    }

    /**
     * Resets login attempts for the given username and IP.
     *
     * @param username
     *            the username
     * @param ip
     *            the IP address
     */
    public void recordSuccess(final String username, final String ip) {
        this.attemptsCache.remove(this.buildKey(username, ip));
    }

    /**
     * Returns the delay in seconds for the next login attempt.
     *
     * @param username
     *            the username
     * @param ip
     *            the IP address
     * @return the delay in seconds
     */
    public long getDelay(final String username, final String ip) {
        final Entry entry = this.attemptsCache.get(this.buildKey(username, ip));
        if (entry == null) {
            return 0;
        }
        final int attempts = entry.attempts();
        if (attempts < BACKOFF_THRESHOLD) {
            return 0;
        }

        // Exponential backoff: start slowing down after BACKOFF_THRESHOLD failures.
        // The shift is clamped to MAX_SHIFT so the result is bounded and can never overflow.
        final int shift = Math.min(attempts - BACKOFF_THRESHOLD, MAX_SHIFT);
        return 1L << shift;
    }

    private void cleanupIfNeeded() {
        if (this.operationCount.incrementAndGet() % CLEANUP_INTERVAL == 0) {
            final Instant cutoff = Instant.now().minus(ENTRY_TTL);
            this.attemptsCache.entrySet().removeIf(entry -> entry.getValue().isIdleSince(cutoff));
        }
    }

    private String buildKey(final String username, final String ip) {
        return username + ":" + ip;
    }

    private static final class Entry {
        private final AtomicInteger attempts = new AtomicInteger();
        private volatile Instant lastAccessed = Instant.now();

        void recordFailure() {
            this.attempts.incrementAndGet();
            this.lastAccessed = Instant.now();
        }

        int attempts() {
            return this.attempts.get();
        }

        boolean isIdleSince(final Instant cutoff) {
            return this.lastAccessed.isBefore(cutoff);
        }
    }
}
