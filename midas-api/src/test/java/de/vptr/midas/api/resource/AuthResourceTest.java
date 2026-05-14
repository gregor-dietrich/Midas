package de.vptr.midas.api.resource;

import static de.vptr.midas.api.util.TestUtil.authenticatedRequest;
import static de.vptr.midas.api.util.TestUtil.given;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class AuthResourceTest {
    private static final String ENDPOINT_URL = "/api/v1/auth";

    @Test
    void testAuthCheckEndpoint_validAuth() {
        authenticatedRequest().when().head(ENDPOINT_URL).then().statusCode(200);
    }

    @Test
    void testAuthCheckEndpoint_noAuth() {
        given().when().head(ENDPOINT_URL).then().statusCode(401);
    }

    @Test
    void testAuthCheckEndpoint_invalidUser() {
        given().auth().basic("root", "admin").when().head(ENDPOINT_URL).then().statusCode(401);
    }

    @Test
    void testAuthCheckEndpoint_invalidPassword() {
        given().auth().basic("admin", "root").when().head(ENDPOINT_URL).then().statusCode(401);
    }

    @Test
    void testAuthCheckEndpoint_malformedBasicHeader() {
        given().header("Authorization", "Basic !!!not-base64!!!").when().head(ENDPOINT_URL).then().statusCode(401);
    }
}
