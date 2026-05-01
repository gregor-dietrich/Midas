package de.vptr.midas.api.security;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.enterprise.context.ApplicationScoped;

/** Tracks failed authentication attempts and enforces token-bucket lockout windows. */
@ApplicationScoped
public class AuthFailureRateLimitService {

    private static final String UNKNOWN_KEY = "__unknown__";
    private static final long CLEANUP_INTERVAL = 64;
    private static final long BUCKET_TTL_MULTIPLIER = 2;

    private final ConcurrentHashMap<String, BucketEntry> failedAuthBuckets = new ConcurrentHashMap<>();
    private final AtomicLong operationCount = new AtomicLong();

    @ConfigProperty(name = "midas.rate-limit.auth-failure.permits", defaultValue = "10")
    long authFailurePermits;

    @ConfigProperty(name = "midas.rate-limit.period-seconds", defaultValue = "60")
    long periodSeconds;

    /**
     * Checks whether the given key is currently blocked due to too many
     * failed authentication attempts.
     *
     * @param key the client identifier (e.g. username or IP address)
     * @return true if the key is blocked
     */
    public boolean isBlocked(final String key) {
        this.cleanupIfNeeded();
        final var entry = getBucketEntry(normalizeKey(key));
        entry.touch();
        return entry.bucket().getAvailableTokens() <= 0;
    }

    /**
     * Records a failed authentication attempt for the given key.
     *
     * @param key the client identifier (e.g. username or IP address)
     */
    public void recordFailure(final String key) {
        this.cleanupIfNeeded();
        final var entry = getBucketEntry(normalizeKey(key));
        entry.touch();
        entry.bucket().tryConsume(1);
    }

    private BucketEntry getBucketEntry(final String key) {
        return this.failedAuthBuckets.computeIfAbsent(key, ignored -> new BucketEntry(createBucket()));
    }

    private void cleanupIfNeeded() {
        if (this.operationCount.incrementAndGet() % CLEANUP_INTERVAL == 0) {
            cleanupExpiredBuckets();
        }
    }

    private void cleanupExpiredBuckets() {
        final var now = Instant.now();
        final var ttl = Duration.ofSeconds(this.periodSeconds * BUCKET_TTL_MULTIPLIER);
        this.failedAuthBuckets.entrySet().removeIf(entry -> entry.getValue().isExpired(now, ttl));
    }

    private static String normalizeKey(final String key) {
        if (key == null || key.isBlank()) {
            return UNKNOWN_KEY;
        }
        return key;
    }

    private Bucket createBucket() {
        final var bandwidth = Bandwidth.builder()
                .capacity(this.authFailurePermits)
                .refillGreedy(this.authFailurePermits, Duration.ofSeconds(this.periodSeconds))
                .build();
        return Bucket.builder().addLimit(bandwidth).build();
    }

    private static final class BucketEntry {
        private final Bucket bucket;
        private volatile Instant lastAccessed;

        BucketEntry(final Bucket bucket) {
            this.bucket = bucket;
            this.lastAccessed = Instant.now();
        }

        void touch() {
            this.lastAccessed = Instant.now();
        }

        boolean isExpired(final Instant now, final Duration ttl) {
            return this.lastAccessed.plus(ttl).isBefore(now);
        }

        Bucket bucket() {
            return this.bucket;
        }
    }
}
