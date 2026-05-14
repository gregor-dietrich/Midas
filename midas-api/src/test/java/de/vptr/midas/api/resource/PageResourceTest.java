package de.vptr.midas.api.resource;

import static de.vptr.midas.api.util.TestDataBuilder.createDefaultPageJson;
import static de.vptr.midas.api.util.TestDataBuilder.createEmptyJson;
import static de.vptr.midas.api.util.TestDataBuilder.createInvalidJson;
import static de.vptr.midas.api.util.TestDataBuilder.createMalformedJson;
import static de.vptr.midas.api.util.TestDataBuilder.createUpdatedPageJson;
import static de.vptr.midas.api.util.TestUtil.authenticatedJsonRequest;
import static de.vptr.midas.api.util.TestUtil.authenticatedRequest;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedGetWithJson;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedGetWithNonExistentResource;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPostWithCreation;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPostWithInsufficientRole;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPostWithSyntaxError;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPostWithValidationError;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedSearchWithJson;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedAccess;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedPost;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedPut;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class PageResourceTest {
    private static final String ENDPOINT_URL = "/api/v1/pages";

    @Test
    void testGetAllPages_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL);
    }

    @Test
    void testGetAllPages_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL);
    }

    @Test
    void testGetPageById_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/1");
    }

    @Test
    void testGetPageById_authorizedWithExistingPage() {
        final String pageId = authenticatedJsonRequest().body(createDefaultPageJson()).when().post(ENDPOINT_URL).then()
                .statusCode(201).extract().path("publicId");
        authenticatedRequest().when().get(ENDPOINT_URL + "/" + pageId).then().statusCode(200);
    }

    @Test
    void testGetPageById_authorizedWithNonExistentPage() {
        testAuthorizedGetWithNonExistentResource(ENDPOINT_URL + "/999");
    }

    @Test
    void testSearchPages_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/search");
    }

    @Test
    void testSearchPages_authorized() {
        testAuthorizedSearchWithJson(ENDPOINT_URL + "/search");
    }

    @Test
    void testCreatePage_unauthorized() {
        testUnauthorizedPost(ENDPOINT_URL, createDefaultPageJson());
    }

    @Test
    void testCreatePage_authorizedWithSufficientRole() {
        testAuthorizedPostWithCreation(ENDPOINT_URL, createDefaultPageJson());
    }

    @Test
    void testCreatePage_authorizedWithInsufficientRole() {
        testAuthorizedPostWithInsufficientRole(ENDPOINT_URL, createDefaultPageJson());
    }

    @Test
    void testCreatePage_authorizedWithEmptyJson() {
        testAuthorizedPostWithValidationError(ENDPOINT_URL, createEmptyJson());
    }

    @Test
    void testCreatePage_authorizedWithInvalidJson() {
        testAuthorizedPostWithValidationError(ENDPOINT_URL, createInvalidJson());
    }

    @Test
    void testCreatePage_authorizedWithMalformedJson() {
        testAuthorizedPostWithSyntaxError(ENDPOINT_URL, createMalformedJson());
    }

    @Test
    void testUpdatePage_unauthorized() {
        testUnauthorizedPut(ENDPOINT_URL + "/1", createUpdatedPageJson());
    }

    @Test
    void testDeletePage_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/1", "DELETE");
    }

    @Test
    void testUpdatePage_authorized() {
        final String pageId = authenticatedJsonRequest().body(createDefaultPageJson()).when().post(ENDPOINT_URL).then()
                .statusCode(201).extract().path("publicId");

        authenticatedJsonRequest().body(createUpdatedPageJson()).when().put(ENDPOINT_URL + "/" + pageId).then()
                .statusCode(200);
    }

    @Test
    void testPatchPage_authorized() {
        final String pageId = authenticatedJsonRequest().body(createDefaultPageJson()).when().post(ENDPOINT_URL).then()
                .statusCode(201).extract().path("publicId");

        authenticatedJsonRequest().body(createDefaultPageJson()).when().patch(ENDPOINT_URL + "/" + pageId).then()
                .statusCode(200);
    }

    @Test
    void testDeletePage_authorized() {
        final String pageId = authenticatedJsonRequest().body(createDefaultPageJson()).when().post(ENDPOINT_URL).then()
                .statusCode(201).extract().path("publicId");

        authenticatedRequest().when().delete(ENDPOINT_URL + "/" + pageId).then().statusCode(204);
    }

    @Test
    void testDeletePage_nonexistent() {
        authenticatedRequest().when().delete(ENDPOINT_URL + "/999999").then().statusCode(404);
    }
}
