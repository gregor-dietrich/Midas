package de.vptr.midas.api.service;

import static de.vptr.midas.api.util.ServiceTestUtil.assertServiceNotNull;
import static de.vptr.midas.api.util.ServiceTestUtil.setupTestCategory;
import static de.vptr.midas.api.util.ServiceTestUtil.setupTestUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;

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

    @SuppressWarnings("NullAway")
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
        final var newComment =
                ServiceTestDataBuilder.createUniquePostCommentDto("Test Comment Content", this.testPost.id);

        final PostCommentResponseDto createdComment =
                this.postCommentService.createComment(newComment, Objects.requireNonNull(this.testUser.username));

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
        final var newComment = ServiceTestDataBuilder.createUniquePostCommentDto("Original Comment", this.testPost.id);
        final PostCommentResponseDto createdComment =
                this.postCommentService.createComment(newComment, Objects.requireNonNull(this.testUser.username));

        final var updateDto = new PostCommentDto();
        updateDto.content = "Updated Comment Content";

        final Long commentId = Objects.requireNonNull(createdComment.id);
        final PostCommentResponseDto updatedComment = this.postCommentService.updateComment(commentId, updateDto,
                Objects.requireNonNull(this.testUser.username));

        assertNotNull(updatedComment);
        assertEquals("Updated Comment Content", updatedComment.content);
    }

    @Test
    @TestTransaction
    void testUpdateCommentShouldDenyNonOwnerWithoutAdminAccess() {
        final var newComment = ServiceTestDataBuilder.createUniquePostCommentDto("Original Comment", this.testPost.id);
        final PostCommentResponseDto createdComment =
                this.postCommentService.createComment(newComment, Objects.requireNonNull(this.testUser.username));
        final var intruder = this.userService.createUser(ServiceTestDataBuilder.createUniqueUserDto());

        final var updateDto = new PostCommentDto();
        updateDto.content = "Blocked update";

        final Long commentId = Objects.requireNonNull(createdComment.id);
        final String intruderUsername = Objects.requireNonNull(intruder.username);
        final var exception = assertThrows(WebApplicationException.class,
                () -> this.postCommentService.updateComment(commentId, updateDto, intruderUsername));
        assertEquals(403, exception.getResponse().getStatus());
    }

    @Test
    @TestTransaction
    void testDeleteComment() {
        final var newComment = ServiceTestDataBuilder.createUniquePostCommentDto("Comment to delete", this.testPost.id);
        final PostCommentResponseDto createdComment =
                this.postCommentService.createComment(newComment, Objects.requireNonNull(this.testUser.username));

        final Long commentId = Objects.requireNonNull(createdComment.id);

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
        final var newComment =
                ServiceTestDataBuilder.createUniquePostCommentDto("Find By ID Comment", this.testPost.id);
        final PostCommentResponseDto createdComment =
                this.postCommentService.createComment(newComment, Objects.requireNonNull(this.testUser.username));

        final Long commentId = Objects.requireNonNull(createdComment.id);
        final var foundComment = this.postCommentService.findById(commentId);

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
        final String username = Objects.requireNonNull(this.testUser.username);
        final Long postId = Objects.requireNonNull(this.testPost.id);
        final var comment1 = ServiceTestDataBuilder.createUniquePostCommentDto("First comment", postId);
        this.postCommentService.createComment(comment1, username);

        final var comment2 = ServiceTestDataBuilder.createUniquePostCommentDto("Second comment", postId);
        this.postCommentService.createComment(comment2, username);

        final var postComments = this.postCommentService.findByPostId(postId);

        assertNotNull(postComments);
        assertTrue(postComments.size() >= 2);

        for (final var comment : postComments) {
            assertEquals(postId, comment.postId);
        }
    }

    @Test
    @TestTransaction
    void testFindByUserId() {
        final String username2 = Objects.requireNonNull(this.testUser.username);
        final Long userId = Objects.requireNonNull(this.testUser.id);
        final var comment1 = ServiceTestDataBuilder.createUniquePostCommentDto("User comment 1", this.testPost.id);
        this.postCommentService.createComment(comment1, username2);

        final var comment2 = ServiceTestDataBuilder.createUniquePostCommentDto("User comment 2", this.testPost.id);
        this.postCommentService.createComment(comment2, username2);

        final var userComments = this.postCommentService.findByUserId(userId);
        assertNotNull(userComments);
        assertTrue(userComments.size() >= 2);

        for (final var comment : userComments) {
            assertEquals(userId, comment.userId);
        }
    }

    @Test
    @TestTransaction
    void testFindRecentComments() {
        final var comment1 = ServiceTestDataBuilder.createUniquePostCommentDto("Recent comment", this.testPost.id);
        this.postCommentService.createComment(comment1, Objects.requireNonNull(this.testUser.username));

        final var recentComments = this.postCommentService.findRecentComments(10);

        assertNotNull(recentComments);
        assertTrue(recentComments.size() >= 1);
    }
}
