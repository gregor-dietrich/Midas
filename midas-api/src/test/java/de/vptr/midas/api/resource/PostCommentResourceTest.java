package de.vptr.midas.api.resource;

import static de.vptr.midas.api.util.TestDataBuilder.createEmptyJson;
import static de.vptr.midas.api.util.TestDataBuilder.createInvalidJson;
import static de.vptr.midas.api.util.TestDataBuilder.createMalformedJson;

import java.util.Objects;

import org.junit.jupiter.api.Test;

import de.vptr.midas.api.util.TestDataBuilder;
import de.vptr.midas.api.util.TestUtil;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class PostCommentResourceTest {
    private static final String ENDPOINT_URL = "/api/v1/comments";

    @Test
    void testGetAllPostComments_unauthorized() {
        TestUtil.testUnauthorizedAccess(ENDPOINT_URL);
    }

    @Test
    void testGetAllPostComments_authorized() {
        TestUtil.testAuthorizedGetWithJson(ENDPOINT_URL);
    }

    @Test
    void testGetCommentById_unauthorized() {
        TestUtil.testUnauthorizedAccess(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7");
    }

    @Test
    @TestTransaction
    void testGetCommentById_authorizedWithExistingComment() {
        // Create a comment first to ensure it exists
        final String postId = TestUtil.createTestPost();
        final String commentId = TestUtil.createTestComment(postId);

        TestUtil.authenticatedRequest().when().get(ENDPOINT_URL + "/" + commentId).then().statusCode(200);
    }

    @Test
    void testGetCommentById_authorizedWithNonExistentComment() {
        TestUtil.testAuthorizedGetWithNonExistentResource(ENDPOINT_URL + "/999");
    }

    @Test
    void testGetCommentsByPost_unauthorized() {
        TestUtil.testUnauthorizedAccess(ENDPOINT_URL + "/post/01AN4Z07BY79KA1307SR9X4MV7");
    }

    @Test
    void testGetCommentsByPost_authorized() {
        TestUtil.testAuthorizedGetWithJson(ENDPOINT_URL + "/post/01AN4Z07BY79KA1307SR9X4MV7");
    }

    @Test
    void testCreateComment_unauthorized() {
        final String commentJson = TestDataBuilder.createDefaultPostCommentJson("1");
        TestUtil.testUnauthorizedPost(ENDPOINT_URL, commentJson);
    }

    @Test
    void testCreateComment_authorized() {
        final String postId = TestUtil.createTestPost();
        final String commentJson = TestDataBuilder.createDefaultPostCommentJson(Objects.requireNonNull(postId));
        TestUtil.testAuthorizedPostWithCreation(ENDPOINT_URL, commentJson);
    }

    @Test
    void testCreateComment_authorizedWithInsufficientRole() {
        final String postId = TestUtil.createTestPost();
        final String commentJson = TestDataBuilder.createDefaultPostCommentJson(Objects.requireNonNull(postId));
        TestUtil.testAuthorizedPostWithInsufficientRole(ENDPOINT_URL, commentJson);
    }

    @Test
    void testCreateComment_authorizedWithEmptyJson() {
        TestUtil.testAuthorizedPostWithValidationError(ENDPOINT_URL, createEmptyJson());
    }

    @Test
    void testCreateComment_authorizedWithInvalidJson() {
        TestUtil.testAuthorizedPostWithValidationError(ENDPOINT_URL, createInvalidJson());
    }

    @Test
    void testCreateComment_authorizedWithMalformedJson() {
        TestUtil.testAuthorizedPostWithSyntaxError(ENDPOINT_URL, createMalformedJson());
    }

    @Test
    void testUpdateComment_unauthorized() {
        final String commentJson = TestDataBuilder.createUpdatedPostCommentJson();
        TestUtil.testUnauthorizedPut(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7", commentJson);
    }

    @Test
    void testUpdateComment_authorized() {
        // First create a comment to update
        final String postId = TestUtil.createTestPost();
        final String commentId = TestUtil.createTestComment(postId);

        // Now update the comment
        final String updateCommentJson = TestDataBuilder.createUpdatedPostCommentJson();
        TestUtil.testAuthorizedPutWithJson(ENDPOINT_URL + "/" + commentId, updateCommentJson);
    }

    @Test
    void testPatchComment_unauthorized() {
        final String commentJson = TestDataBuilder.createPatchedPostCommentJson();
        TestUtil.testUnauthorizedPatch(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7", commentJson);
    }

    @Test
    void testPatchComment_authorized() {
        // First create a comment to patch
        final String postId = TestUtil.createTestPost();
        final String commentId = TestUtil.createTestComment(postId);

        // Now patch the comment
        final String patchCommentJson = TestDataBuilder.createPatchedPostCommentJson();
        TestUtil.testAuthorizedPatchWithJson(ENDPOINT_URL + "/" + commentId, patchCommentJson);
    }

    @Test
    void testDeleteComment_unauthorized() {
        TestUtil.testUnauthorizedAccess(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7", "DELETE");
    }

    @Test
    void testDeleteComment_authorized() {
        final String postId = TestUtil.createTestPost();
        final String commentId = TestUtil.createTestComment(postId);
        TestUtil.authenticatedRequest().when().delete(ENDPOINT_URL + "/" + commentId).then().statusCode(204);
    }

    @Test
    void testGetCommentsByUser_unauthorized() {
        TestUtil.testUnauthorizedAccess(ENDPOINT_URL + "/user/01AN4Z07BY79KA1307SR9X4MV7");
    }

    @Test
    void testGetCommentsByUser_authorized() {
        TestUtil.testAuthorizedGetWithJson(ENDPOINT_URL + "/user/01AN4Z07BY79KA1307SR9X4MV7");
    }

    @Test
    void testGetRecentComments_unauthorized() {
        TestUtil.testUnauthorizedAccess(ENDPOINT_URL + "/recent");
    }

    @Test
    void testGetRecentComments_authorized() {
        TestUtil.testAuthorizedGetWithJson(ENDPOINT_URL + "/recent");
    }

    @Test
    void testSearchComments_unauthorized() {
        TestUtil.testUnauthorizedAccess(ENDPOINT_URL + "/search?query=test");
    }

    @Test
    void testSearchComments_authorized() {
        TestUtil.testAuthorizedGetWithJson(ENDPOINT_URL + "/search?query=test");
        TestUtil.authenticatedRequest().when().get(ENDPOINT_URL + "/search?query=nonexistentxyz").then()
                .statusCode(200);
    }

    @Test
    void testGetCommentsByDateRange_unauthorized() {
        TestUtil.testUnauthorizedAccess(ENDPOINT_URL + "/date-range?startDate=2024-01-01&endDate=2024-12-31");
    }

    @Test
    void testGetCommentsByDateRange_authorized() {
        TestUtil.testAuthorizedGetWithJson(ENDPOINT_URL + "/date-range?startDate=2024-01-01&endDate=2024-12-31");
    }

    @Test
    void testGetCommentsByDateRange_missingParams() {
        TestUtil.testAuthorizedGetWithJson(ENDPOINT_URL + "/date-range");
    }

    @Test
    void testDeleteComment_nonexistent() {
        TestUtil.authenticatedRequest().when().delete(ENDPOINT_URL + "/999999").then().statusCode(404);
    }
}
