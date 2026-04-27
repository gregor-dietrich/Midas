package de.vptr.midas.api.security;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.enterprise.context.ApplicationScoped;

/** Tracks failed authentication attempts and enforces token-bucket lockout windows. */
@ApplicationScoped
public class AuthFailureRateLimitService {

    private static final String UNKNOWN_KEY = "__unknown__";

    private final ConcurrentHashMap<String, Bucket> failedAuthBuckets = new ConcurrentHashMap<>();

    @ConfigProperty(name = "midas.rate-limit.auth-failure.permits", defaultValue = "10")
    long authFailurePermits;

    @ConfigProperty(name = "midas.rate-limit.period-seconds", defaultValue = "60")
    long periodSeconds;

    public boolean isBlocked(final String key) {
        return getBucket(normalizeKey(key)).getAvailableTokens() <= 0;
    }

    public void recordFailure(final String key) {
        getBucket(normalizeKey(key)).tryConsume(1);
    }

    private Bucket getBucket(final String key) {
        return this.failedAuthBuckets.computeIfAbsent(key, ignored -> createBucket());
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
}
