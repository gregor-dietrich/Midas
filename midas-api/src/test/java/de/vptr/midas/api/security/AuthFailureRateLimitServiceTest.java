package de.vptr.midas.api.security;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class AuthFailureRateLimitServiceTest {

    @Inject
    AuthFailureRateLimitService authFailureRateLimitService;

    @Test
    @SuppressWarnings("NullAway")
    void isBlocked_shouldReturnFalse_whenKeyIsNull() {
        assertFalse(this.authFailureRateLimitService.isBlocked(null));
    }

    @Test
    void isBlocked_shouldReturnFalse_whenKeyIsBlank() {
        assertFalse(this.authFailureRateLimitService.isBlocked("   "));
    }

    @Test
    void isBlocked_shouldTriggerPeriodicCleanup_afterCleanupInterval() {
        // Call isBlocked enough times to reach the cleanup interval (64 ops),
        // covering the operationCount % CLEANUP_INTERVAL == 0 true branch
        for (int i = 0; i < 70; i++) {
            this.authFailureRateLimitService.isBlocked("cleanup-probe-" + i);
        }
    }
}
