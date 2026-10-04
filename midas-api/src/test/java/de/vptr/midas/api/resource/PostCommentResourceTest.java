package de.vptr.midas.api.resource;

import static de.vptr.midas.api.util.TestDataBuilder.createEmptyJson;
import static de.vptr.midas.api.util.TestDataBuilder.createInvalidJson;
import static de.vptr.midas.api.util.TestDataBuilder.createMalformedJson;
import static de.vptr.midas.api.util.TestUtil.authenticatedJsonRequest;
import static de.vptr.midas.api.util.TestUtil.authenticatedRequest;
import static de.vptr.midas.api.util.TestUtil.createTestCategory;
import static de.vptr.midas.api.util.TestUtil.createTestComment;
import static de.vptr.midas.api.util.TestUtil.createTestPost;
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
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedPatch;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedPost;
import static de.vptr.midas.api.util.TestUtil.testUnauthorizedPut;
import static org.hamcrest.Matchers.equalTo;

import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import de.vptr.midas.api.util.TestDataBuilder;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class PostCommentResourceTest {
    private static final String ENDPOINT_URL = "/api/v1/comments";

    @Test
    void testGetAllPostComments_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL);
    }

    @Test
    void testGetAllPostComments_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL);
    }

    @Test
    void testGetCommentById_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7");
    }

    @Test
    @TestTransaction
    void testGetCommentById_authorizedWithExistingComment() {
        // Create a comment first to ensure it exists
        final String postId = createTestPost();
        final String commentId = createTestComment(postId);

        authenticatedRequest().when().get(ENDPOINT_URL + "/" + commentId).then().statusCode(200);
    }

    @Test
    void testGetCommentById_authorizedWithNonExistentComment() {
        testAuthorizedGetWithNonExistentResource(ENDPOINT_URL + "/999");
    }

    @Test
    void testGetCommentsByPost_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/post/01AN4Z07BY79KA1307SR9X4MV7");
    }

    @Test
    void testGetCommentsByPost_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL + "/post/01AN4Z07BY79KA1307SR9X4MV7");
    }

    @Test
    void testCreateComment_unauthorized() {
        final String commentJson = TestDataBuilder.createDefaultPostCommentJson("1");
        testUnauthorizedPost(ENDPOINT_URL, commentJson);
    }

    @Test
    void testCreateComment_authorized() {
        final String postId = createTestPost();
        final String commentJson = TestDataBuilder.createDefaultPostCommentJson(Objects.requireNonNull(postId));
        testAuthorizedPostWithCreation(ENDPOINT_URL, commentJson);
    }

    @ParameterizedTest
    @CsvSource({ "false,true,Cannot add comment to an unpublished post.",
            "true,false,Comments are not allowed on this post." })
    void testCreateComment_refusedWith422(final boolean published, final boolean commentable, final String message) {
        final String postJson = TestDataBuilder.createPostJson("Test Post", "Test content", published, commentable,
                "01AN4Z07BY79KA1307SR9X4MV7", Objects.requireNonNull(createTestCategory()));
        final String postId = authenticatedJsonRequest().body(postJson).when().post("/api/v1/posts").then()
                .statusCode(201).extract().path("publicId");
        final String commentJson = TestDataBuilder.createDefaultPostCommentJson(Objects.requireNonNull(postId));
        authenticatedJsonRequest().body(commentJson).when().post(ENDPOINT_URL).then().statusCode(422)
                .body("status", equalTo(422)).body("message", equalTo(message));
    }

    @Test
    void testCreateComment_authorizedWithInsufficientRole() {
        final String postId = createTestPost();
        final String commentJson = TestDataBuilder.createDefaultPostCommentJson(Objects.requireNonNull(postId));
        testAuthorizedPostWithInsufficientRole(ENDPOINT_URL, commentJson);
    }

    @Test
    void testCreateComment_authorizedWithEmptyJson() {
        testAuthorizedPostWithValidationError(ENDPOINT_URL, createEmptyJson());
    }

    @Test
    void testCreateComment_authorizedWithInvalidJson() {
        testAuthorizedPostWithValidationError(ENDPOINT_URL, createInvalidJson());
    }

    @Test
    void testCreateComment_authorizedWithMalformedJson() {
        testAuthorizedPostWithSyntaxError(ENDPOINT_URL, createMalformedJson());
    }

    @Test
    void testUpdateComment_unauthorized() {
        final String commentJson = TestDataBuilder.createUpdatedPostCommentJson();
        testUnauthorizedPut(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7", commentJson);
    }

    @Test
    void testUpdateComment_authorized() {
        // First create a comment to update
        final String postId = createTestPost();
        final String commentId = createTestComment(postId);

        // Now update the comment
        final String updateCommentJson = TestDataBuilder.createUpdatedPostCommentJson();
        testAuthorizedPutWithJson(ENDPOINT_URL + "/" + commentId, updateCommentJson);
    }

    @Test
    void testPatchComment_unauthorized() {
        final String commentJson = TestDataBuilder.createPatchedPostCommentJson();
        testUnauthorizedPatch(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7", commentJson);
    }

    @Test
    void testPatchComment_authorized() {
        // First create a comment to patch
        final String postId = createTestPost();
        final String commentId = createTestComment(postId);

        // Now patch the comment
        final String patchCommentJson = TestDataBuilder.createPatchedPostCommentJson();
        testAuthorizedPatchWithJson(ENDPOINT_URL + "/" + commentId, patchCommentJson);
    }

    @Test
    void testDeleteComment_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/01AN4Z07BY79KA1307SR9X4MV7", "DELETE");
    }

    @Test
    void testDeleteComment_authorized() {
        final String postId = createTestPost();
        final String commentId = createTestComment(postId);
        authenticatedRequest().when().delete(ENDPOINT_URL + "/" + commentId).then().statusCode(204);
    }

    @Test
    void testGetCommentsByUser_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/user/01AN4Z07BY79KA1307SR9X4MV7");
    }

    @Test
    void testGetCommentsByUser_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL + "/user/01AN4Z07BY79KA1307SR9X4MV7");
    }

    @Test
    void testGetRecentComments_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/recent");
    }

    @Test
    void testGetRecentComments_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL + "/recent");
    }

    @Test
    void testSearchComments_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/search?query=test");
    }

    @Test
    void testSearchComments_authorized() {
        testAuthorizedSearchWithJson(ENDPOINT_URL + "/search");
    }

    @Test
    void testSearchComments_queryTooLong() {
        final String longQuery = "a".repeat(101);
        authenticatedRequest().when().get(ENDPOINT_URL + "/search?query=" + longQuery).then().statusCode(400);
    }

    @Test
    void testGetCommentsByDateRange_unauthorized() {
        testUnauthorizedAccess(ENDPOINT_URL + "/date-range?startDate=2024-01-01&endDate=2024-12-31");
    }

    @Test
    void testGetCommentsByDateRange_authorized() {
        testAuthorizedGetWithJson(ENDPOINT_URL + "/date-range?startDate=2024-01-01&endDate=2024-12-31");
    }

    @Test
    void testGetCommentsByDateRange_missingParams() {
        testAuthorizedGetWithJson(ENDPOINT_URL + "/date-range");
    }

    @Test
    void testDeleteComment_nonexistent() {
        authenticatedRequest().when().delete(ENDPOINT_URL + "/999999").then().statusCode(404);
    }
}
