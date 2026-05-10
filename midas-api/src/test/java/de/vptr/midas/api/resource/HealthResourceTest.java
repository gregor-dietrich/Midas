package de.vptr.midas.api.resource;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import org.junit.jupiter.api.Test;

import de.vptr.midas.api.util.TestUtil;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class HealthResourceTest {

    @Test
    void testLivenessEndpoint() {
        TestUtil.given().when().get("/q/health/live").then().statusCode(200).body("status", equalTo("UP"))
                .body("checks.name", hasItem("midas-api"));
    }

    @Test
    void testReadinessEndpoint() {
        TestUtil.given().when().get("/q/health/ready").then().statusCode(200).body("status", equalTo("UP"));
    }

    @Test
    void testCombinedHealthEndpoint() {
        TestUtil.given().when().get("/q/health").then().statusCode(200).body("status", equalTo("UP"));
    }
}
