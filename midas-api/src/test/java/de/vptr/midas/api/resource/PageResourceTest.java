package de.vptr.midas.api.resource;

import static de.vptr.midas.api.util.TestDataBuilder.createEmptyJson;
import static de.vptr.midas.api.util.TestDataBuilder.createInvalidJson;
import static de.vptr.midas.api.util.TestDataBuilder.createMalformedJson;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.vptr.midas.api.util.TestDataBuilder;
import de.vptr.midas.api.util.TestUtil;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class PageResourceTest {
    private static final String ENDPOINT_URL = "/api/v1/pages";

    @Test
    void testGetAllPages_permitAll() {
        TestUtil.testPermitAllGetWithJson(ENDPOINT_URL);
    }

    @Test
    void testGetAllPages_authorized() {
        TestUtil.testAuthorizedGetWithJson(ENDPOINT_URL);
    }

    @Test
    void testGetPageById_permitAll() {
        // Endpoint is @PermitAll — verify no 401. Page may or may not exist in test
        // data.
        final int statusCode = TestUtil.given()
                .when()
                .get(ENDPOINT_URL + "/1")
                .then()
                .extract().statusCode();
        assertTrue(statusCode == 200 || statusCode == 404,
                "Expected 200 or 404 but got " + statusCode);
    }

    @Test
    void testGetPageById_authorizedWithExistingPage() {
        TestUtil.testAuthorizedGetWithExistingResource(ENDPOINT_URL + "/1");
    }

    @Test
    void testGetPageById_authorizedWithNonExistentPage() {
        TestUtil.testAuthorizedGetWithNonExistentResource(ENDPOINT_URL + "/999");
    }

    @Test
    void testSearchPages_blankQueryReturnsFirstBatch() {
        TestUtil.given()
                .queryParam("query", "   ")
                .when()
                .get(ENDPOINT_URL + "/search")
                .then()
                .statusCode(200);
    }

    @Test
    void testCreatePage_unauthorized() {
        final String pageJson = TestDataBuilder.createDefaultPageJson();
        TestUtil.testUnauthorizedPost(ENDPOINT_URL, pageJson);
    }

    @Test
    void testCreatePage_authorizedWithSufficientRole() {
        final String pageJson = TestDataBuilder.createDefaultPageJson();
        TestUtil.testAuthorizedPostWithCreation(ENDPOINT_URL, pageJson);
    }

    @Test
    void testCreatePage_authorizedWithInsufficientRole() {
        final String pageJson = TestDataBuilder.createDefaultPageJson();
        TestUtil.testAuthorizedPostWithInsufficientRole(ENDPOINT_URL, pageJson);
    }

    @Test
    void testCreatePage_authorizedWithEmptyJson() {
        TestUtil.testAuthorizedPostWithValidationError(ENDPOINT_URL, createEmptyJson());
    }

    @Test
    void testCreatePage_authorizedWithInvalidJson() {
        TestUtil.testAuthorizedPostWithValidationError(ENDPOINT_URL, createInvalidJson());
    }

    @Test
    void testCreatePage_authorizedWithMalformedJson() {
        TestUtil.testAuthorizedPostWithSyntaxError(ENDPOINT_URL, createMalformedJson());
    }

    @Test
    void testUpdatePage_unauthorized() {
        final String pageJson = TestDataBuilder.createUpdatedPageJson();
        TestUtil.testUnauthorizedPut(ENDPOINT_URL + "/1", pageJson);
    }

    @Test
    void testDeletePage_unauthorized() {
        TestUtil.testUnauthorizedAccess(ENDPOINT_URL + "/1", "DELETE");
    }
}
