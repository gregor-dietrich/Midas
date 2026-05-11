package de.vptr.midas.api.resource;

import static de.vptr.midas.api.util.TestDataBuilder.createEmptyJson;
import static de.vptr.midas.api.util.TestDataBuilder.createInvalidJson;
import static de.vptr.midas.api.util.TestDataBuilder.createMalformedJson;

import org.junit.jupiter.api.Test;

import de.vptr.midas.api.util.TestDataBuilder;
import de.vptr.midas.api.util.TestUtil;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class PageResourceTest {
    private static final String ENDPOINT_URL = "/api/v1/pages";

    @Test
    void testGetAllPages_unauthorized() {
        TestUtil.testUnauthorizedAccess(ENDPOINT_URL);
    }

    @Test
    void testGetAllPages_authorized() {
        TestUtil.testAuthorizedGetWithJson(ENDPOINT_URL);
    }

    @Test
    void testGetPageById_unauthorized() {
        TestUtil.testUnauthorizedAccess(ENDPOINT_URL + "/1");
    }

    @Test
    void testGetPageById_authorizedWithExistingPage() {
        final Integer pageId = TestUtil.authenticatedRequest().contentType("application/json")
                .body(TestDataBuilder.createDefaultPageJson()).when().post(ENDPOINT_URL).then().statusCode(201)
                .extract().path("id");
        TestUtil.authenticatedRequest().when().get(ENDPOINT_URL + "/" + pageId).then().statusCode(200);
    }

    @Test
    void testGetPageById_authorizedWithNonExistentPage() {
        TestUtil.testAuthorizedGetWithNonExistentResource(ENDPOINT_URL + "/999");
    }

    @Test
    void testSearchPages_unauthorized() {
        TestUtil.testUnauthorizedAccess(ENDPOINT_URL + "/search");
    }

    @Test
    void testSearchPages_authorized() {
        TestUtil.testAuthorizedGetWithJson(ENDPOINT_URL + "/search?query=test");
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

    @Test
    void testUpdatePage_authorized() {
        final Integer pageId = TestUtil.authenticatedRequest().contentType("application/json")
                .body(TestDataBuilder.createDefaultPageJson()).when().post(ENDPOINT_URL).then().statusCode(201)
                .extract().path("id");

        final String updatedJson = TestDataBuilder.createUpdatedPageJson();
        TestUtil.authenticatedRequest().contentType("application/json").body(updatedJson).when()
                .put(ENDPOINT_URL + "/" + pageId).then().statusCode(200);
    }

    @Test
    void testPatchPage_authorized() {
        final Integer pageId = TestUtil.authenticatedRequest().contentType("application/json")
                .body(TestDataBuilder.createDefaultPageJson()).when().post(ENDPOINT_URL).then().statusCode(201)
                .extract().path("id");

        final String pageJson = TestDataBuilder.createDefaultPageJson();
        TestUtil.authenticatedRequest().contentType("application/json").body(pageJson).when()
                .patch(ENDPOINT_URL + "/" + pageId).then().statusCode(200);
    }

    @Test
    void testDeletePage_authorized() {
        final Integer pageId = TestUtil.authenticatedRequest().contentType("application/json")
                .body(TestDataBuilder.createDefaultPageJson()).when().post(ENDPOINT_URL).then().statusCode(201)
                .extract().path("id");

        TestUtil.authenticatedRequest().when().delete(ENDPOINT_URL + "/" + pageId).then().statusCode(204);
    }
}
