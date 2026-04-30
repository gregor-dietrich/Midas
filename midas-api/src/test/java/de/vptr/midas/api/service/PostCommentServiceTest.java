package de.vptr.midas.api.service;

import static de.vptr.midas.api.util.ServiceTestUtil.assertServiceNotNull;
import static de.vptr.midas.api.util.ServiceTestUtil.setupTestCategory;
import static de.vptr.midas.api.util.ServiceTestUtil.setupTestUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.vptr.midas.api.dto.PostCommentDto;
import de.vptr.midas.api.dto.PostCommentResponseDto;
import de.vptr.midas.api.entity.PostCategoryEntity;
import de.vptr.midas.api.entity.PostCommentEntity;
import de.vptr.midas.api.entity.PostEntity;
import de.vptr.midas.api.entity.UserEntity;
import de.vptr.midas.api.util.AbstractIntegrationTest;
import de.vptr.midas.api.util.ServiceTestDataBuilder;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

@QuarkusTest
class PostCommentServiceTest extends AbstractIntegrationTest {
    @Inject
    PostCommentService postCommentService;

    @Inject
    UserService userService;

    @Inject
    PostService postService;

    @Inject
    PostCategoryService postCategoryService;

    private UserEntity testUser;
    private PostEntity testPost;
    private PostCategoryEntity testCategory;

    @BeforeEach
    void setUp() {
        runInIsolatedTransaction(() -> {
            this.testCategory = setupTestCategory();
            this.testUser = setupTestUser(this.userService);

            final var testPostDto = ServiceTestDataBuilder.createUniquePostDto(this.testUser.id, this.testCategory.id);
            testPostDto.published = true;
            testPostDto.commentable = true;
            final var createdPost = this.postService.createPost(testPostDto);
            this.testPost = PostEntity.findById(createdPost.id);
        });
    }

    @AfterEach
    void tearDown() {
        runCleanup(() -> {
            PostCommentEntity.deleteAll();
            PostEntity.deleteAll();
            UserEntity.delete("username like ?1", "testuser_%");
            PostCategoryEntity.deleteAll();
        });
    }

    @Test
    void testServiceNotNull() {
        assertServiceNotNull(this.postCommentService);
    }

    @Test
    void testGetAllComments() {
        final var comments = this.postCommentService.getAllComments();
        assertNotNull(comments);
    }

    @Test
    @TestTransaction
    void testCreateComment() {
        final var newComment = ServiceTestDataBuilder.createUniquePostCommentDto(
                "Test Comment Content", this.testPost.id);

        final PostCommentResponseDto createdComment = this.postCommentService.createComment(newComment,
                this.testUser.username);

        assertNotNull(createdComment);
        assertNotNull(createdComment.id);
        assertEquals("Test Comment Content", createdComment.content);
        assertEquals(this.testUser.id, createdComment.userId);
        assertEquals(this.testPost.id, createdComment.postId);
        assertNotNull(createdComment.created);
    }

    @Test
    @TestTransaction
    void testUpdateComment() {
        // First create a comment
        final var newComment = ServiceTestDataBuilder.createUniquePostCommentDto(
                "Original Comment", this.testPost.id);
        final PostCommentResponseDto createdComment = this.postCommentService.createComment(newComment,
                this.testUser.username);

        // Update the comment
        final var updateDto = new PostCommentDto();
        updateDto.content = "Updated Comment Content";

        final PostCommentResponseDto updatedComment = this.postCommentService.updateComment(createdComment.id, updateDto,
                this.testUser.username);

        assertNotNull(updatedComment);
        assertEquals("Updated Comment Content", updatedComment.content);
    }

    @Test
    @TestTransaction
    void testUpdateCommentShouldDenyNonOwnerWithoutAdminAccess() {
        final var newComment = ServiceTestDataBuilder.createUniquePostCommentDto(
                "Original Comment", this.testPost.id);
        final PostCommentResponseDto createdComment = this.postCommentService.createComment(newComment,
                this.testUser.username);
        final var intruder = this.userService.createUser(ServiceTestDataBuilder.createUniqueUserDto());

        final var updateDto = new PostCommentDto();
        updateDto.content = "Blocked update";

        final var exception = assertThrows(WebApplicationException.class,
                () -> this.postCommentService.updateComment(createdComment.id, updateDto, intruder.username));
        assertEquals(403, exception.getResponse().getStatus());
    }

    @Test
    @TestTransaction
    void testDeleteComment() {
        // First create a comment
        final var newComment = ServiceTestDataBuilder.createUniquePostCommentDto(
                "Comment to delete", this.testPost.id);
        final PostCommentResponseDto createdComment = this.postCommentService.createComment(newComment,
                this.testUser.username);

        final Long commentId = createdComment.id;

        final var deleted = this.postCommentService.deleteComment(commentId, this.testUser.username);

        assertTrue(deleted);
        final var deletedComment = this.postCommentService.findById(commentId);
        assertTrue(deletedComment.isEmpty());
    }

    @Test
    void testDeleteNonExistentComment() {
        final var deleted = this.postCommentService.deleteComment(999999L, null);
        assertFalse(deleted);
    }

    @Test
    @TestTransaction
    void testFindById() {
        // First create a comment
        final var newComment = ServiceTestDataBuilder.createUniquePostCommentDto(
                "Find By ID Comment", this.testPost.id);
        final PostCommentResponseDto createdComment = this.postCommentService.createComment(newComment,
                this.testUser.username);

        final var foundComment = this.postCommentService.findById(createdComment.id);

        assertTrue(foundComment.isPresent());
        assertEquals("Find By ID Comment", foundComment.get().content);
    }

    @Test
    void testFindByIdNonExistent() {
        final var foundComment = this.postCommentService.findById(999999L);
        assertTrue(foundComment.isEmpty());
    }

    @Test
    @TestTransaction
    void testFindByPostId() {
        // First create comments
        final var comment1 = ServiceTestDataBuilder.createUniquePostCommentDto(
                "First comment", this.testPost.id);
        this.postCommentService.createComment(comment1, this.testUser.username);

        final var comment2 = ServiceTestDataBuilder.createUniquePostCommentDto(
                "Second comment", this.testPost.id);
        this.postCommentService.createComment(comment2, this.testUser.username);

        final var postComments = this.postCommentService.findByPostId(this.testPost.id);

        assertNotNull(postComments);
        assertTrue(postComments.size() >= 2);

        // Verify all comments belong to the post
        for (final var comment : postComments) {
            assertEquals(this.testPost.id, comment.postId);
        }
    }

    @Test
    @TestTransaction
    void testFindByUserId() {
        // First create comments
        final var comment1 = ServiceTestDataBuilder.createUniquePostCommentDto(
                "User comment 1", this.testPost.id);
        this.postCommentService.createComment(comment1, this.testUser.username);

        final var comment2 = ServiceTestDataBuilder.createUniquePostCommentDto(
                "User comment 2", this.testPost.id);
        this.postCommentService.createComment(comment2, this.testUser.username);

        final var userComments = this.postCommentService.findByUserId(this.testUser.id);
        assertNotNull(userComments);
        assertTrue(userComments.size() >= 2);

        // Verify all comments belong to the user
        for (final var comment : userComments) {
            assertEquals(this.testUser.id, comment.userId);
        }
    }

    @Test
    @TestTransaction
    void testFindRecentComments() {
        // First create a comment
        final var comment1 = ServiceTestDataBuilder.createUniquePostCommentDto(
                "Recent comment", this.testPost.id);
        this.postCommentService.createComment(comment1, this.testUser.username);

        final var recentComments = this.postCommentService.findRecentComments(10);

        assertNotNull(recentComments);
        assertTrue(recentComments.size() >= 1);
    }
}
