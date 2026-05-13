package de.vptr.midas.api.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Service to track login attempts and implement exponential backoff.
 */
@ApplicationScoped
public class LoginAttemptService {

    private final ConcurrentHashMap<String, AtomicInteger> attemptsCache = new ConcurrentHashMap<>();

    /**
     * Records a failed login attempt for the given username and IP.
     *
     * @param username
     *            the username
     * @param ip
     *            the IP address
     */
    public void recordFailure(final String username, final String ip) {
        final String key = this.buildKey(username, ip);
        this.attemptsCache.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
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
        final String key = this.buildKey(username, ip);
        final int attempts = this.attemptsCache.getOrDefault(key, new AtomicInteger(0)).get();

        if (attempts < 5) {
            return 0;
        }

        // Exponential backoff: start slowing down after 5 failures
        // delay = 1 << (attempts - 5)
        return 1L << (attempts - 5);
    }

    private String buildKey(final String username, final String ip) {
        return username + ":" + ip;
    }
}
