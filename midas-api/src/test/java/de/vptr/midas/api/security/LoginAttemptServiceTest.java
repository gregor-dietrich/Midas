package de.vptr.midas.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

// Literal client IPs are the subject under test here, not a hard-coded endpoint.
@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
@QuarkusTest
class LoginAttemptServiceTest {

    @Inject
    LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        // Reset or use unique keys if necessary, but LoginAttemptService doesn't have a reset all.
        // I'll use a unique username for each test.
    }

    @Test
    void testExponentialBackoff() {
        final String username = "backoffUser";
        final String ip = "127.0.0.1";

        // 0-4 failures: 0s delay
        for (int i = 0; i < 4; i++) {
            this.loginAttemptService.recordFailure(username, ip);
            assertEquals(0, this.loginAttemptService.getDelay(username, ip));
        }

        // 5th failure: 1s delay (1 << 0)
        this.loginAttemptService.recordFailure(username, ip);
        assertEquals(1, this.loginAttemptService.getDelay(username, ip));

        // 6th failure: 2s delay (1 << 1)
        this.loginAttemptService.recordFailure(username, ip);
        assertEquals(2, this.loginAttemptService.getDelay(username, ip));

        // 7th failure: 4s delay (1 << 2)
        this.loginAttemptService.recordFailure(username, ip);
        assertEquals(4, this.loginAttemptService.getDelay(username, ip));

        // Success: 0s delay
        this.loginAttemptService.recordSuccess(username, ip);
        assertEquals(0, this.loginAttemptService.getDelay(username, ip));
    }
}
