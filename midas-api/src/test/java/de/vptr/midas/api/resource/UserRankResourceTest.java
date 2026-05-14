package de.vptr.midas.api.resource;

import static de.vptr.midas.api.util.TestDataBuilder.createEmptyJson;
import static de.vptr.midas.api.util.TestDataBuilder.createInvalidJson;
import static de.vptr.midas.api.util.TestDataBuilder.createMalformedJson;
import static de.vptr.midas.api.util.TestDataBuilder.createUniqueUpdatedUserRankJson;
import static de.vptr.midas.api.util.TestDataBuilder.createUniqueUserRankJson;
import static de.vptr.midas.api.util.TestUtil.authenticatedGuestJsonRequest;
import static de.vptr.midas.api.util.TestUtil.authenticatedGuestRequest;
import static de.vptr.midas.api.util.TestUtil.authenticatedJsonRequest;
import static de.vptr.midas.api.util.TestUtil.authenticatedRequest;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedGetWithExistingResource;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedGetWithJson;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedGetWithNonExistentResource;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPostWithCreation;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPostWithInsufficientRole;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPostWithSyntaxError;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPostWithValidationError;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedSearchWithJson;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedAccess;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedDelete;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedPost;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedPut;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class UserRankResourceTest {
    private static final String ENDPOINT_URL = "/api/v1/user-ranks";

    @Test
    void testGetAllUserRanks_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL);
    }

    @Test
    void testGetAllUserRanks_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL);
    }

    @Test
    void testGetAllUserRanks_authorizedWithInsufficientRole() {
        authenticatedGuestRequest().when().get(ENDPOINT_URL).then().statusCode(403);
    }

    @Test
    void testGetUserRankById_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/1");
    }

    @Test
    void testGetUserRankById_authorizedWithExistingRank() {
        testAuthorizedGetWithExistingResource(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV3");
    }

    @Test
    void testGetUserRankById_authorizedWithNonExistentRank() {
        testAuthorizedGetWithNonExistentResource(ENDPOINT_URL + "/999");
    }

    @Test
    void testGetUserRankById_authorizedWithInsufficientRole() {
        authenticatedGuestRequest().when().get(ENDPOINT_URL + "/1").then().statusCode(403);
    }

    @Test
    void testGetUserRankByName_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/name/testname");
    }

    @Test
    void testGetUserRankByName_authorizedWithInsufficientRole() {
        authenticatedGuestRequest().when().get(ENDPOINT_URL + "/name/testname").then().statusCode(403);
    }

    @Test
    void testGetUserRankByName_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL + "/name/Administrator");
        testAuthorizedGetWithNonExistentResource(ENDPOINT_URL + "/name/nonexistent");
    }

    @Test
    void testSearchUserRanks_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/search?query=test");
    }

    @Test
    void testSearchUserRanks_authorizedWithInsufficientRole() {
        authenticatedGuestRequest().when().get(ENDPOINT_URL + "/search?query=test").then().statusCode(403);
    }

    @Test
    void testSearchUserRanks_authorized() {
        testAuthorizedSearchWithJson(ENDPOINT_URL + "/search");
    }

    @Test
    void testCreateUserRank_unauthorized() {
        testUnauthorizedPost(ENDPOINT_URL, createUniqueUserRankJson());
    }

    @Test
    void testCreateUserRank_authorizedWithSufficientRole() {
        testAuthorizedPostWithCreation(ENDPOINT_URL, createUniqueUserRankJson());
    }

    @Test
    void testCreateUserRank_authorizedWithInsufficientRole() {
        testAuthorizedPostWithInsufficientRole(ENDPOINT_URL, createUniqueUserRankJson());
    }

    @Test
    void testCreateUserRank_authorizedWithEmptyJson() {
        testAuthorizedPostWithValidationError(ENDPOINT_URL, createEmptyJson());
    }

    @Test
    void testCreateUserRank_authorizedWithInvalidJson() {
        testAuthorizedPostWithValidationError(ENDPOINT_URL, createInvalidJson());
    }

    @Test
    void testCreateUserRank_authorizedWithMalformedJson() {
        testAuthorizedPostWithSyntaxError(ENDPOINT_URL, createMalformedJson());
    }

    @Test
    void testUpdateUserRank_unauthorized() {
        testUnauthorizedPut(ENDPOINT_URL + "/1", createUniqueUpdatedUserRankJson());
    }

    @Test
    void testUpdateUserRank_authorizedWithInsufficientRole() {
        authenticatedGuestJsonRequest().body(createUniqueUpdatedUserRankJson()).when().put(ENDPOINT_URL + "/1").then()
                .statusCode(403);
    }

    @Test
    void testUpdateUserRank_authorized() {
        final String id = authenticatedJsonRequest().body(createUniqueUserRankJson()).when().post(ENDPOINT_URL).then()
                .statusCode(201).extract().path("publicId");

        authenticatedJsonRequest().body(createUniqueUpdatedUserRankJson()).when().put(ENDPOINT_URL + "/" + id).then()
                .statusCode(200);

        authenticatedRequest().when().delete(ENDPOINT_URL + "/" + id).then().statusCode(204);
    }

    @Test
    void testPatchUserRank_unauthorized() {
        testUnauthorizedPut(ENDPOINT_URL + "/1", createUniqueUpdatedUserRankJson());
    }

    @Test
    void testPatchUserRank_authorizedWithInsufficientRole() {
        authenticatedGuestJsonRequest().body(createUniqueUpdatedUserRankJson()).when().patch(ENDPOINT_URL + "/1").then()
                .statusCode(403);
    }

    @Test
    void testPatchUserRank_authorized() {
        final String id = authenticatedJsonRequest().body(createUniqueUserRankJson()).when().post(ENDPOINT_URL).then()
                .statusCode(201).extract().path("publicId");

        authenticatedJsonRequest().body(createUniqueUpdatedUserRankJson()).when().patch(ENDPOINT_URL + "/" + id).then()
                .statusCode(200);

        authenticatedRequest().when().delete(ENDPOINT_URL + "/" + id).then().statusCode(204);
    }

    @Test
    void testDeleteUserRank_unauthorized() {
        testUnauthorizedDelete(ENDPOINT_URL + "/1");
    }

    @Test
    void testDeleteUserRank_authorizedWithInsufficientRole() {
        authenticatedGuestRequest().when().delete(ENDPOINT_URL + "/1").then().statusCode(403);
    }

    @Test
    void testDeleteUserRank_authorized() {
        final String id = authenticatedJsonRequest().body(createUniqueUserRankJson()).when().post(ENDPOINT_URL).then()
                .statusCode(201).extract().path("publicId");

        authenticatedRequest().when().delete(ENDPOINT_URL + "/" + id).then().statusCode(204);
    }

    @Test
    void testDeleteUserRank_nonexistent() {
        authenticatedRequest().when().delete(ENDPOINT_URL + "/999999").then().statusCode(404);
    }
}
