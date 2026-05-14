package de.vptr.midas.api.resource;

import static de.vptr.midas.api.util.TestDataBuilder.createDefaultPostJson;
import static de.vptr.midas.api.util.TestDataBuilder.createDefaultPostPatchJson;
import static de.vptr.midas.api.util.TestDataBuilder.createEmptyJson;
import static de.vptr.midas.api.util.TestDataBuilder.createInvalidJson;
import static de.vptr.midas.api.util.TestDataBuilder.createMalformedJson;
import static de.vptr.midas.api.util.TestDataBuilder.createUpdatedPostJson;
import static de.vptr.midas.api.util.TestUtil.authenticatedGuestRequest;
import static de.vptr.midas.api.util.TestUtil.authenticatedRequest;
import static de.vptr.midas.api.util.TestUtil.createTestPost;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedGetWithExistingResourceAndJson;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedGetWithJson;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedGetWithNonExistentResource;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPatchWithJson;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPostWithCreation;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPostWithInsufficientRole;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPostWithSyntaxError;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPostWithValidationError;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedPutWithJson;
import static de.vptr.midas.api.util.TestUtil.testAuthorizedSearchWithJson;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedAccess;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedDelete;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedPatch;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedPost;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedPut;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class PostResourceTest {
    private static final String ENDPOINT_URL = "/api/v1/posts";

    @Test
    void testGetAllPosts_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL);
    }

    @Test
    void testGetAllPosts_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL);
    }

    @Test
    void testGetPublishedPosts_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/published");
    }

    @Test
    void testGetPublishedPosts_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL + "/published");
    }

    @Test
    void testGetPost_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7");
    }

    @Test
    void testGetPost_authorizedWithExistingPost() {
        final String id = createTestPost();
        testAuthorizedGetWithExistingResourceAndJson(ENDPOINT_URL + "/" + id);
    }

    @Test
    void testGetPost_authorizedWithNonExistentPost() {
        testAuthorizedGetWithNonExistentResource(ENDPOINT_URL + "/999");
    }

    @Test
    void testGetPostsByUser_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/user/01AN4Z07BY79KA1307SR9X4MV7");
    }

    @Test
    void testGetPostsByUserAndCategory_authorized() {
        createTestPost();
        testAuthorizedGetWithJson(ENDPOINT_URL + "/user/01AN4Z07BY79KA1307SR9X4MV7");
        final String categoryId = authenticatedRequest().when().get(ENDPOINT_URL).then().statusCode(200).extract()
                .path("[0].category.publicId");
        if (categoryId != null) {
            testAuthorizedGetWithJson(ENDPOINT_URL + "/category/" + categoryId);
        }
    }

    @Test
    void testSearchPosts_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/search?query=test");
    }

    @Test
    void testSearchPosts_authorized() {
        testAuthorizedSearchWithJson(ENDPOINT_URL + "/search");
    }

    @Test
    void testGetPostsByDateRange_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/date-range?startDate=2024-01-01&endDate=2024-12-31");
    }

    @Test
    void testGetPostsByDateRange_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL + "/date-range?startDate=2024-01-01&endDate=2024-12-31");
    }

    @Test
    void testGetPostsByDateRange_missingParams() {
        testAuthorizedGetWithJson(ENDPOINT_URL + "/date-range");
        testAuthorizedGetWithJson(ENDPOINT_URL + "/date-range?startDate=2024-01-01");
        testAuthorizedGetWithJson(ENDPOINT_URL + "/date-range?endDate=2024-12-31");
    }

    @Test
    void testGetAllPostsByUserAdmin_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/user/01AN4Z07BY79KA1307SR9X4MV7/admin");
    }

    @Test
    void testGetAllPostsByUserAdmin_forbiddenForGuest() {
        authenticatedGuestRequest().when().get(ENDPOINT_URL + "/user/01AN4Z07BY79KA1307SR9X4MV7/admin").then()
                .statusCode(403);
    }

    @Test
    void testGetAllPostsByUserAdmin_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL + "/user/01AN4Z07BY79KA1307SR9X4MV7/admin");
    }

    @Test
    void testGetAllPostsByDateRangeAdmin_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/date-range/admin?startDate=2024-01-01&endDate=2024-12-31");
    }

    @Test
    void testGetAllPostsByDateRangeAdmin_forbiddenForGuest() {
        authenticatedGuestRequest().when()
                .get(ENDPOINT_URL + "/date-range/admin?startDate=2024-01-01&endDate=2024-12-31").then().statusCode(403);
    }

    @Test
    void testGetAllPostsByDateRangeAdmin_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL + "/date-range/admin?startDate=2024-01-01&endDate=2024-12-31");
    }

    @Test
    void testCreatePost_unauthorized() {
        testUnauthorizedPost(ENDPOINT_URL, createDefaultPostJson());
    }

    @Test
    void testCreatePost_authorizedWithSufficientRole() {
        testAuthorizedPostWithCreation(ENDPOINT_URL, createDefaultPostJson());
    }

    @Test
    void testCreatePost_authorizedWithInsufficientRole() {
        testAuthorizedPostWithInsufficientRole(ENDPOINT_URL, createDefaultPostJson());
    }

    @Test
    void testCreatePost_authorizedWithEmptyJson() {
        testAuthorizedPostWithValidationError(ENDPOINT_URL, createEmptyJson());
    }

    @Test
    void testCreatePost_authorizedWithInvalidJson() {
        testAuthorizedPostWithValidationError(ENDPOINT_URL, createInvalidJson());
    }

    @Test
    void testCreatePost_authorizedWithMalformedJson() {
        testAuthorizedPostWithSyntaxError(ENDPOINT_URL, createMalformedJson());
    }

    @Test
    void testUpdatePost_unauthorized() {
        testUnauthorizedPut(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7", createUpdatedPostJson());
    }

    @Test
    void testUpdatePost_authorized() {
        final String postId = createTestPost();
        testAuthorizedPutWithJson(ENDPOINT_URL + "/" + postId, createUpdatedPostJson());
    }

    @Test
    void testPatchPost_unauthorized() {
        testUnauthorizedPatch(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7", createDefaultPostPatchJson());
    }

    @Test
    void testPatchPost_authorized() {
        final String postId = createTestPost();
        testAuthorizedPatchWithJson(ENDPOINT_URL + "/" + postId, createDefaultPostPatchJson());
    }

    @Test
    void testDeletePost_unauthorized() {
        testUnauthorizedDelete(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7");
    }

    @Test
    void testDeletePost_authorized() {
        final String postId = createTestPost();
        authenticatedRequest().when().delete(ENDPOINT_URL + "/" + postId).then().statusCode(204);
    }

    @Test
    void testDeletePost_nonexistent() {
        authenticatedRequest().when().delete(ENDPOINT_URL + "/999999").then().statusCode(404);
    }

    @Test
    void testGetPostsByUser_nonexistent() {
        authenticatedRequest().when().get(ENDPOINT_URL + "/user/999999").then().statusCode(200).body("size()",
                equalTo(0));
    }
}
