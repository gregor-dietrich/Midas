package de.vptr.midas.api.resource;

import static org.hamcrest.Matchers.equalTo;

import java.util.Map;

import org.junit.jupiter.api.Test;

import de.vptr.midas.api.util.TestUtil;
import io.restassured.http.ContentType;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(RateLimitFilterTest.RateLimitTestProfile.class)
class RateLimitFilterTest {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String REAL_IP_HEADER = "X-Real-IP";

    @Test
    void testReadRequestsRateLimitedByIpAndEndpoint() {
        final String ip = "198.51.100.11";
        for (int i = 0; i < 5; ++i) {
            TestUtil.authenticatedRequest()
                    .header(FORWARDED_FOR_HEADER, ip)
                    .when()
                    .get("/api/v1/posts/published")
                    .then()
                    .statusCode(200);
        }

        assertRateLimitResponse(ip, "/api/v1/posts/published", "GET", "Read request limit exceeded");
    }

    @Test
    void testWriteRequestsRateLimitedByIpAndEndpoint() {
        final String ip = "198.51.100.12";
        for (int i = 0; i < 3; ++i) {
            TestUtil.given()
                    .header(FORWARDED_FOR_HEADER, ip)
                    .when()
                    .delete("/api/v1/posts/1")
                    .then()
                    .statusCode(401);
        }

        assertRateLimitResponse(ip, "/api/v1/posts/1", "DELETE", "Write request limit exceeded");
    }

    @Test
    void testAuthRequestsRateLimitedByIpAndEndpoint() {
        final String ip = "198.51.100.14";
        for (int i = 0; i < 2; ++i) {
            TestUtil.given()
                    .header(FORWARDED_FOR_HEADER, ip)
                    .when()
                    .head("/api/v1/auth")
                    .then()
                    .statusCode(401);
        }

        TestUtil.given()
                .header(FORWARDED_FOR_HEADER, ip)
                .when()
                .head("/api/v1/auth")
                .then()
                .statusCode(429);
    }

    @Test
    void testAuthRateLimitUsesStructuredErrorPayload() {
        final String ip = "198.51.100.15";
        for (int i = 0; i < 2; ++i) {
            TestUtil.given()
                    .header(FORWARDED_FOR_HEADER, ip)
                    .when()
                    .get("/api/v1/auth");
        }

        assertRateLimitResponse(ip, "/api/v1/auth", "GET", "Authentication request limit exceeded");
    }

    @Test
    void testForwardedProxyChainUsesFirstClientHop() {
        final String firstClientChain = "198.51.100.16, 10.0.0.5";
        final String secondClientChain = "198.51.100.17, 10.0.0.5";
        for (int i = 0; i < 5; ++i) {
            TestUtil.authenticatedRequest()
                    .header(FORWARDED_FOR_HEADER, firstClientChain)
                    .when()
                    .get("/api/v1/posts/published")
                    .then()
                    .statusCode(200);
        }

        assertRateLimitResponse(firstClientChain, "/api/v1/posts/published", "GET", "Read request limit exceeded");

        TestUtil.authenticatedRequest()
                .header(FORWARDED_FOR_HEADER, secondClientChain)
                .when()
                .get("/api/v1/posts/published")
                .then()
                .statusCode(200);
    }

    @Test
    void testMalformedForwardedForFallsBackToRemoteAddress() {
        final String malformedForwardedFor = "198.51.100.18:8443";
        for (int i = 0; i < 5; ++i) {
            TestUtil.authenticatedRequest()
                    .header(FORWARDED_FOR_HEADER, malformedForwardedFor)
                    .when()
                    .get("/api/v1/posts/published")
                    .then()
                    .statusCode(200);
        }

        assertRateLimitResponseWithoutForwardedHeaders("/api/v1/posts/published", "GET", "Read request limit exceeded");
    }

    @Test
    void testMalformedRealIpFallsBackToRemoteAddress() {
        final String malformedRealIp = "203.0.113.51:443";
        for (int i = 0; i < 5; ++i) {
            TestUtil.given()
                    .header(REAL_IP_HEADER, malformedRealIp)
                    .when()
                    .get("/api/v1/users")
                    .then()
                    .statusCode(401);
        }

        assertRateLimitResponseWithoutForwardedHeaders("/api/v1/users", "GET", "Read request limit exceeded");
    }

    @Test
    void testReadBucketEvictsOldestClientWhenCapacityExceeded() {
        final String firstIp = "198.51.100.21";
        final String secondIp = "198.51.100.22";
        final String thirdIp = "198.51.100.23";

        for (int i = 0; i < 5; ++i) {
            TestUtil.authenticatedRequest()
                    .header(FORWARDED_FOR_HEADER, firstIp)
                    .when()
                    .get("/api/v1/posts/published")
                    .then()
                    .statusCode(200);
        }
        TestUtil.authenticatedRequest()
                .header(FORWARDED_FOR_HEADER, firstIp)
                .when()
                .get("/api/v1/posts/published")
                .then()
                .statusCode(429);

        TestUtil.authenticatedRequest()
                .header(FORWARDED_FOR_HEADER, secondIp)
                .when()
                .get("/api/v1/posts/published")
                .then()
                .statusCode(200);

        TestUtil.authenticatedRequest()
                .header(FORWARDED_FOR_HEADER, thirdIp)
                .when()
                .get("/api/v1/posts/published")
                .then()
                .statusCode(200);

        TestUtil.authenticatedRequest()
                .header(FORWARDED_FOR_HEADER, firstIp)
                .when()
                .get("/api/v1/posts/published")
                .then()
                .statusCode(200);
    }

    @Test
    void testInvalidBasicAuthAttemptsAreThrottled() {
        final String ip = "198.51.100.13";
        for (int i = 0; i < 2; ++i) {
            TestUtil.given()
                    .header(FORWARDED_FOR_HEADER, ip)
                    .auth().preemptive().basic("invalid_user", "invalid_password")
                    .when()
                    .get("/api/v1/users")
                    .then()
                    .statusCode(401);
        }

        TestUtil.given()
                .header(FORWARDED_FOR_HEADER, ip)
                .auth().preemptive().basic("invalid_user", "invalid_password")
                .when()
                .get("/api/v1/users")
                .then()
                .statusCode(429)
                .contentType(ContentType.JSON)
                .header("Retry-After", equalTo("60"))
                .body("status", equalTo(429))
                .body("error", equalTo("Too Many Requests"))
                .body("message", equalTo("Too many invalid authentication attempts"));
    }

    private static void assertRateLimitResponse(final String ip, final String path, final String method,
            final String message) {
        TestUtil.given()
                .header(FORWARDED_FOR_HEADER, ip)
                .when()
                .request(method, path)
                .then()
                .statusCode(429)
                .contentType(ContentType.JSON)
                .header("Retry-After", equalTo("60"))
                .body("status", equalTo(429))
                .body("error", equalTo("Too Many Requests"))
                .body("message", equalTo(message));
    }

    private static void assertRateLimitResponseWithoutForwardedHeaders(final String path, final String method,
            final String message) {
        TestUtil.given()
                .when()
                .request(method, path)
                .then()
                .statusCode(429)
                .contentType(ContentType.JSON)
                .header("Retry-After", equalTo("60"))
                .body("status", equalTo(429))
                .body("error", equalTo("Too Many Requests"))
                .body("message", equalTo(message));
    }

    public static class RateLimitTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "midas.rate-limit.read.permits", "5",
                    "midas.rate-limit.write.permits", "3",
                    "midas.rate-limit.auth.permits", "2",
                    "midas.rate-limit.auth-failure.permits", "2",
                    "midas.rate-limit.period-seconds", "60",
                    "midas.rate-limit.bucket.max-entries", "2",
                    // Tests simulate a trusted reverse-proxy environment
                    "rate-limit.trust-forwarded-for", "true");
        }
    }
}
