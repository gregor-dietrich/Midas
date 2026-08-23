package de.vptr.midas.api.resource;

import static de.vptr.midas.api.util.TestUtil.assertRateLimitResponse;

import java.util.Map;

import org.junit.jupiter.api.Test;

import de.vptr.midas.api.util.TestUtil;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.annotation.Nullable;

// Literal client IPs are the subject under test here, not a hard-coded endpoint.
@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
@QuarkusTest
@TestProfile(RateLimitFilterTest.RateLimitTestProfile.class)
class RateLimitFilterTest {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String REAL_IP_HEADER = "X-Real-IP";

    @Test
    void testReadRequestsRateLimitedByIpAndEndpoint() {
        final String ip = "198.51.100.11";
        for (int i = 0; i < 5; ++i) {
            TestUtil.forwardedAuthenticatedRequest(ip).when().get("/api/v1/posts/published").then().statusCode(200);
        }

        assertRateLimit(ip, "/api/v1/posts/published", "GET", "Read request limit exceeded");
    }

    @Test
    void testWriteRequestsRateLimitedByIpAndEndpoint() {
        final String ip = "198.51.100.12";
        for (int i = 0; i < 3; ++i) {
            TestUtil.given().header(FORWARDED_FOR_HEADER, ip).when().delete("/api/v1/posts/1").then().statusCode(401);
        }

        assertRateLimit(ip, "/api/v1/posts/1", "DELETE", "Write request limit exceeded");
    }

    @Test
    void testAuthRequestsRateLimitedByIpAndEndpoint() {
        final String ip = "198.51.100.14";
        for (int i = 0; i < 2; ++i) {
            TestUtil.given().header(FORWARDED_FOR_HEADER, ip).when().head("/api/v1/auth").then().statusCode(401);
        }

        TestUtil.given().header(FORWARDED_FOR_HEADER, ip).when().head("/api/v1/auth").then().statusCode(429);
    }

    @Test
    void testAuthRateLimitUsesStructuredErrorPayload() {
        final String ip = "198.51.100.15";
        for (int i = 0; i < 2; ++i) {
            TestUtil.given().header(FORWARDED_FOR_HEADER, ip).when().get("/api/v1/auth");
        }

        assertRateLimit(ip, "/api/v1/auth", "GET", "Authentication request limit exceeded");
    }

    @Test
    void testForwardedProxyChainUsesFirstClientHop() {
        final String firstClientChain = "198.51.100.16, 10.0.0.5";
        final String secondClientChain = "198.51.100.17, 10.0.0.5";
        for (int i = 0; i < 5; ++i) {
            TestUtil.forwardedAuthenticatedRequest(firstClientChain).when().get("/api/v1/posts/published").then()
                    .statusCode(200);
        }

        assertRateLimit(firstClientChain, "/api/v1/posts/published", "GET", "Read request limit exceeded");

        TestUtil.forwardedAuthenticatedRequest(secondClientChain).when().get("/api/v1/posts/published").then()
                .statusCode(200);
    }

    @Test
    void testMalformedForwardedForFallsBackToRemoteAddress() {
        final String malformedForwardedFor = "198.51.100.18:8443";
        for (int i = 0; i < 5; ++i) {
            TestUtil.forwardedAuthenticatedRequest(malformedForwardedFor).when().get("/api/v1/posts/published").then()
                    .statusCode(200);
        }

        assertRateLimitWithoutForwardedHeaders("/api/v1/posts/published", "GET", "Read request limit exceeded");
    }

    @Test
    void testMalformedRealIpFallsBackToRemoteAddress() {
        final String malformedRealIp = "203.0.113.51:443";
        for (int i = 0; i < 5; ++i) {
            TestUtil.given().header(REAL_IP_HEADER, malformedRealIp).when().get("/api/v1/users").then().statusCode(401);
        }

        assertRateLimitWithoutForwardedHeaders("/api/v1/users", "GET", "Read request limit exceeded");
    }

    @Test
    void testReadBucketEvictsOldestClientWhenCapacityExceeded() {
        final String firstIp = "198.51.100.21";
        final String secondIp = "198.51.100.22";
        final String thirdIp = "198.51.100.23";

        for (int i = 0; i < 5; ++i) {
            TestUtil.forwardedAuthenticatedRequest(firstIp).when().get("/api/v1/posts/published").then()
                    .statusCode(200);
        }
        TestUtil.forwardedAuthenticatedRequest(firstIp).when().get("/api/v1/posts/published").then().statusCode(429);

        TestUtil.forwardedAuthenticatedRequest(secondIp).when().get("/api/v1/posts/published").then().statusCode(200);

        TestUtil.forwardedAuthenticatedRequest(thirdIp).when().get("/api/v1/posts/published").then().statusCode(200);

        TestUtil.forwardedAuthenticatedRequest(firstIp).when().get("/api/v1/posts/published").then().statusCode(200);
    }

    @Test
    void testInvalidBasicAuthAttemptsAreThrottled() {
        final String ip = "198.51.100.13";
        for (int i = 0; i < 2; ++i) {
            TestUtil.given().header(FORWARDED_FOR_HEADER, ip).auth().preemptive()
                    .basic("invalid_user", "invalid_password").when().get("/api/v1/users").then().statusCode(401);
        }

        final var response = TestUtil.given().header(FORWARDED_FOR_HEADER, ip).auth().preemptive()
                .basic("invalid_user", "invalid_password").when().get("/api/v1/users").then();
        assertRateLimitResponse(response, "Too many invalid authentication attempts");
    }

    @Test
    void testInvalidBasicAuthAttemptsAreThrottledByRealIp() {
        final String ip = "198.51.100.15";
        for (int i = 0; i < 2; ++i) {
            TestUtil.given().header(REAL_IP_HEADER, ip).auth().preemptive().basic("another_user", "invalid_password")
                    .when().get("/api/v1/users").then().statusCode(401);
        }

        TestUtil.given().header(REAL_IP_HEADER, ip).auth().preemptive().basic("another_user", "invalid_password").when()
                .get("/api/v1/users").then().statusCode(429);
    }

    @Test
    void testInvalidBasicAuthAttemptsAreThrottledByRemoteAddress() {
        for (int i = 0; i < 2; ++i) {
            TestUtil.given().auth().preemptive().basic("yet_another_user", "invalid_password").when()
                    .get("/api/v1/users").then().statusCode(401);
        }

        TestUtil.given().auth().preemptive().basic("yet_another_user", "invalid_password").when().get("/api/v1/users")
                .then().statusCode(429);
    }

    @Test
    void testInvalidBasicAuthAttemptsAreThrottledWithBlankForwardedFor() {
        for (int i = 0; i < 2; ++i) {
            TestUtil.given().header(FORWARDED_FOR_HEADER, "  ").auth().preemptive()
                    .basic("blank_user", "invalid_password").when().get("/api/v1/users").then().statusCode(401);
        }

        TestUtil.given().header(FORWARDED_FOR_HEADER, "  ").auth().preemptive().basic("blank_user", "invalid_password")
                .when().get("/api/v1/users").then().statusCode(429);
    }

    @Test
    void testInvalidBasicAuthAttemptsAreThrottledWithMultipleForwardedFor() {
        for (int i = 0; i < 2; ++i) {
            TestUtil.given().header(FORWARDED_FOR_HEADER, "198.51.100.16, 10.0.0.1").auth().preemptive()
                    .basic("multi_user", "invalid_password").when().get("/api/v1/users").then().statusCode(401);
        }

        TestUtil.given().header(FORWARDED_FOR_HEADER, "198.51.100.16, 10.0.0.1").auth().preemptive()
                .basic("multi_user", "invalid_password").when().get("/api/v1/users").then().statusCode(429);
    }

    private static void assertRateLimit(@Nullable final String ip, final String path, final String method,
            final String message) {
        final var request = (ip != null) ? TestUtil.given().header(FORWARDED_FOR_HEADER, ip) : TestUtil.given();
        final var response = request.when().request(method, path).then();
        assertRateLimitResponse(response, message);
    }

    private static void assertRateLimitWithoutForwardedHeaders(final String path, final String method,
            final String message) {
        assertRateLimit(null, path, method, message);
    }

    public static class RateLimitTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of("midas.rate-limit.read.permits", "5", "midas.rate-limit.write.permits", "3",
                    "midas.rate-limit.auth.permits", "2", "midas.rate-limit.auth-failure.permits", "2",
                    "midas.rate-limit.period-seconds", "60", "midas.rate-limit.bucket.max-entries", "2",
                    // Tests simulate a trusted reverse-proxy environment from localhost
                    "app.security.trusted-proxy-ips", "127.0.0.1,::1,0:0:0:0:0:0:0:1");
        }
    }
}
