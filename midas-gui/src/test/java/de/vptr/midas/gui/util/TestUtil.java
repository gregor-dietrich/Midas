package de.vptr.midas.gui.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class for GUI service tests with common patterns and assertions
 */
public class TestUtil {

    private static final AtomicLong UNIQUE_COUNTER = new AtomicLong(System.currentTimeMillis());

    private TestUtil() {
        // Utility class
    }

    /**
     * Generate a unique test suffix to avoid conflicts in test data
     */
    public static String generateUniqueTestSuffix() {
        return String.valueOf(UNIQUE_COUNTER.incrementAndGet());
    }

    /**
     * Assert that a service is not null
     */
    public static void assertServiceNotNull(final Object service) {
        assertThat(service).isNotNull();
    }

    /**
     * Standard auth header for tests
     */
    public static final String TEST_AUTH_HEADER = "Basic dGVzdDp0ZXN0";

    /**
     * Creates a unique test username with prefix
     */
    public static String createUniqueTestUsername(final String prefix) {
        return prefix + "_" + generateUniqueTestSuffix();
    }

    /**
     * Creates a unique test email with prefix
     */
    public static String createUniqueTestEmail(final String prefix) {
        return prefix + "_" + generateUniqueTestSuffix() + "@example.com";
    }

    /**
     * Creates a unique test name for entities
     */
    public static String createUniqueTestName(final String prefix) {
        return prefix + " " + generateUniqueTestSuffix();
    }
}
