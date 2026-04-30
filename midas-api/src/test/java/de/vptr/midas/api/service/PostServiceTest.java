package de.vptr.midas.api.service;

import static de.vptr.midas.api.util.ServiceTestUtil.assertServiceNotNull;
import static de.vptr.midas.api.util.ServiceTestUtil.cleanupTestData;
import static de.vptr.midas.api.util.ServiceTestUtil.generateUniqueTestSuffix;
import static de.vptr.midas.api.util.ServiceTestUtil.setupTestCategory;
import static de.vptr.midas.api.util.ServiceTestUtil.setupTestUser;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.vptr.midas.api.dto.PostDto;
import de.vptr.midas.api.dto.PostResponseDto;
import de.vptr.midas.api.entity.PostCategoryEntity;
import de.vptr.midas.api.entity.PostCommentEntity;
import de.vptr.midas.api.entity.PostEntity;
import de.vptr.midas.api.entity.UserEntity;
import de.vptr.midas.api.util.AbstractIntegrationTest;
import de.vptr.midas.api.util.PaginationUtil;
import de.vptr.midas.api.util.ServiceTestDataBuilder;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;

@QuarkusTest
class PostServiceTest extends AbstractIntegrationTest {
    @Inject
    PostService postService;

    @Inject
    UserService userService;

    private UserEntity testUser;
    private PostCategoryEntity testCategory;

    @BeforeEach
    void setUp() {
        runCleanup(() -> {
            PostCommentEntity.deleteAll();
            cleanupTestData(PostEntity.class);
            this.cleanupTestUsers();

            this.testUser = setupTestUser(this.userService);
            this.testCategory = setupTestCategory();
        });
    }

    private void cleanupTestUsers() {
        // Clean up any test users that start with "testuser_" prefix
        UserEntity.delete("username like ?1", "testuser_%");
    }

    @AfterEach
    void tearDown() {
        runCleanup(() -> {
            PostCommentEntity.deleteAll();
            cleanupTestData(PostEntity.class);
            this.cleanupTestUsers();
        });
    }

    @Test
    void testServiceNotNull() {
        assertServiceNotNull(this.postService);
    }

    @Test
    void testGetAllPosts() {
        final var posts = this.postService.getAllPosts();
        assertNotNull(posts);
    }

    @Test
    void testFindPublishedPosts() {
        final var publishedPosts = this.postService.findPublishedPosts();
        assertNotNull(publishedPosts);

        // All returned posts should be published
        for (final PostResponseDto post : publishedPosts) {
            assertTrue(post.published);
        }
    }

    @Test
    @TestTransaction
    void testCreatePost() {
        final var newPost = ServiceTestDataBuilder.createUniquePostDto(this.testUser.id, this.testCategory.id);

        final var createdPost = this.postService.createPost(newPost);

        assertNotNull(createdPost);
        assertNotNull(createdPost.id);
        assertEquals(newPost.title, createdPost.title);
        assertEquals(newPost.content, createdPost.content);
        assertNotNull(createdPost.created);
        assertNull(createdPost.lastEdit);
        assertFalse(createdPost.published); // Default should be false
        assertFalse(createdPost.commentable); // Default should be false
    }

    @Test
    @TestTransaction
    void testCreatePostWithPublishedFlag() {
        final var newPost = ServiceTestDataBuilder.createPostDto(
                "Published Test Post",
                "Published test content",
                true,
                true,
                this.testUser.id,
                this.testCategory.id);

        final var createdPost = this.postService.createPost(newPost);

        assertNotNull(createdPost);
        assertTrue(createdPost.published);
        assertTrue(createdPost.commentable);
    }

    @Test
    @TestTransaction
    void testUpdatePost() {
        // First create a post
        final var newPost = ServiceTestDataBuilder.createPostDto(
                "Original Title",
                "Original content",
                false,
                false,
                this.testUser.id,
                this.testCategory.id);
        final var createdPost = this.postService.createPost(newPost);

        final var originalCreated = createdPost.created;

        // Wait a bit to ensure timestamp difference
        try {
            Thread.sleep(10);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Update the post
        final var updateDto = ServiceTestDataBuilder.createPostDto(
                "Updated Title",
                "Updated content",
                false,
                false,
                null,
                null);

        final var updatedPost = this.postService.updatePost(createdPost.id, updateDto, this.testUser.username);

        assertNotNull(updatedPost);
        assertEquals("Updated Title", updatedPost.title);
        assertEquals("Updated content", updatedPost.content);
        assertEquals(originalCreated, updatedPost.created);
        assertNotNull(updatedPost.lastEdit);
        assertTrue(updatedPost.lastEdit.isAfter(originalCreated));
    }

    @Test
    @TestTransaction
    void testUpdatePostShouldDenyNonOwnerWithoutAdminAccess() {
        final var newPost = ServiceTestDataBuilder.createPostDto(
                "Ownership Title",
                "Ownership content",
                false,
                false,
                this.testUser.id,
                this.testCategory.id);
        final var createdPost = this.postService.createPost(newPost);
        final var intruder = this.userService.createUser(ServiceTestDataBuilder.createUniqueUserDto());

        final var updateDto = ServiceTestDataBuilder.createPostDto(
                "Blocked update",
                "Blocked content",
                false,
                false,
                null,
                null);

        final var exception = assertThrows(WebApplicationException.class,
                () -> this.postService.updatePost(createdPost.id, updateDto, intruder.username));
        assertEquals(403, exception.getResponse().getStatus());
    }

    @Test
    @TestTransaction
    void testPatchPost() {
        // First create a post
        final var newPost = ServiceTestDataBuilder.createPostDto(
                "Original Title",
                "Original content",
                false,
                false,
                this.testUser.id,
                this.testCategory.id);
        final var createdPost = this.postService.createPost(newPost);

        // Patch only the title
        final var patchDto = ServiceTestDataBuilder.createPostDto(
                "Patched Title",
                null,
                false,
                false,
                null,
                null);

        final var patchedPost = this.postService.patchPost(createdPost.id, patchDto, this.testUser.username);

        assertNotNull(patchedPost);
        assertEquals("Patched Title", patchedPost.title);
        assertEquals("Original content", patchedPost.content); // Content should remain unchanged
    }

    @Test
    @TestTransaction
    void testDeletePost() {
        // First create a post
        final var newPost = ServiceTestDataBuilder.createPostDto(
                "To Be Deleted",
                "This post will be deleted",
                false,
                false,
                this.testUser.id,
                this.testCategory.id);
        final var createdPost = this.postService.createPost(newPost);

        final var postId = createdPost.id;

        final var deleted = this.postService.deletePost(postId, this.testUser.username);

        assertTrue(deleted);
        final var deletedPost = this.postService.findById(postId);
        assertTrue(deletedPost.isEmpty());
    }

    @Test
    void testDeleteNonExistentPost() {
        final var deleted = this.postService.deletePost(999999L, null);
        assertFalse(deleted);
    }

    @Test
    @TestTransaction
    void testFindById() {
        // First create a post
        final var newPost = ServiceTestDataBuilder.createPostDto(
                "Find By ID Test",
                "Test content",
                false,
                false,
                this.testUser.id,
                this.testCategory.id);
        final var createdPost = this.postService.createPost(newPost);

        final var foundPost = this.postService.findById(createdPost.id);

        assertTrue(foundPost.isPresent());
        assertEquals("Find By ID Test", foundPost.get().title);
    }

    @Test
    void testFindByIdNonExistent() {
        final var foundPost = this.postService.findById(999999L);
        assertTrue(foundPost.isEmpty());
    }

    @Test
    @TestTransaction
    void testFindByUserId() {
        // First create a post
        final var newPost = ServiceTestDataBuilder.createPostDto(
                "User Post Test",
                "Test content",
                false,
                false,
                this.testUser.id,
                this.testCategory.id);
        this.postService.createPost(newPost);

        final var userPosts = this.postService.findByUserId(this.testUser.id);

        assertNotNull(userPosts);
        assertFalse(userPosts.isEmpty());
        assertTrue(userPosts.stream().allMatch(post -> post.userId.equals(this.testUser.id)));
    }

    @Test
    @TestTransaction
    void testFindByCategoryId() {
        // First create a post
        final var newPost = ServiceTestDataBuilder.createPostDto(
                "Category Post Test",
                "Test content",
                false,
                false,
                this.testUser.id,
                this.testCategory.id);
        this.postService.createPost(newPost);

        final var categoryPosts = this.postService.findByCategoryId(this.testCategory.id);

        assertNotNull(categoryPosts);
        assertFalse(categoryPosts.isEmpty());
        assertTrue(categoryPosts.stream().allMatch(post -> post.categoryId.equals(this.testCategory.id)));
    }

    @Test
    void testSearchPostsWithNullOrBlankQueryUsesDefaultListing() {
        final var expected = this.postService.getAllPosts();

        final var nullResults = this.postService.searchPosts(null);
        final var blankResults = this.postService.searchPosts("   ");

        assertEquals(expected.stream().map(post -> post.id).toList(),
                nullResults.stream().map(post -> post.id).toList());
        assertEquals(expected.stream().map(post -> post.id).toList(),
                blankResults.stream().map(post -> post.id).toList());
    }

    @Test
    @TestTransaction
    void testSearchPostsWithQuery() {
        final var dto = ServiceTestDataBuilder.createPostDto("UniqueSearchTitle999", "content", false, false,
                this.testUser.id,
                this.testCategory.id);
        this.postService.createPost(dto);

        final var results = this.postService.searchPosts("UniqueSearchTitle999");
        assertNotNull(results);
        assertFalse(results.isEmpty());
    }

    @Test
    @TestTransaction
    void testSearchPostsAppliesSharedResultLimit() {
        final String token = "post-limit-" + generateUniqueTestSuffix();
        for (int i = 0; i < PaginationUtil.MAX_SIZE + 5; i++) {
            final var post = ServiceTestDataBuilder.createPostDto(
                    token + "-" + i,
                    "content-" + i,
                    false,
                    false,
                    this.testUser.id,
                    this.testCategory.id);
            this.postService.createPost(post);
        }

        final var results = this.postService.searchPosts(token);

        assertEquals(PaginationUtil.MAX_SIZE, results.size());
    }

    @Test
    void testFindByDateRangeWithNullStart() {
        final var results = this.postService.findByDateRange(null, "2025-01-31");
        assertNotNull(results);
    }

    @Test
    void testFindByDateRangeWithNullEnd() {
        final var results = this.postService.findByDateRange("2025-01-01", null);
        assertNotNull(results);
    }

    @Test
    @TestTransaction
    void testFindByDateRangeValid() {
        final var dto = ServiceTestDataBuilder.createPostDto("Date Range Test", "content", false, false,
                this.testUser.id,
                this.testCategory.id);
        this.postService.createPost(dto);

        final var results = this.postService.findByDateRange(
                LocalDate.now().minusDays(1).toString(),
                LocalDate.now().plusDays(1).toString());
        assertNotNull(results);
        assertFalse(results.isEmpty());
    }

    @Test
    void testFindByDateRangeInvalidFormat() {
        assertThrows(ValidationException.class, () -> this.postService.findByDateRange("bad-date", "2025-01-01"));
    }

    @Test
    @TestTransaction
    void testUpdatePostNotFound() {
        final var dto = ServiceTestDataBuilder.createPostDto("Title", "Content", false, false, this.testUser.id, null);
        assertThrows(WebApplicationException.class, () -> this.postService.updatePost(999999L, dto, null));
    }

    @Test
    @TestTransaction
    void testUpdatePostWithBlankTitle() {
        final var createDto = ServiceTestDataBuilder.createPostDto("OrigTitle", "Content", false, false,
                this.testUser.id,
                this.testCategory.id);
        final var created = this.postService.createPost(createDto);

        final var dto = ServiceTestDataBuilder.createPostDto("", "Content", false, false, this.testUser.id, null);
        assertThrows(ValidationException.class,
                () -> this.postService.updatePost(created.id, dto, this.testUser.username));
    }

    @Test
    @TestTransaction
    void testUpdatePostWithBlankContent() {
        final var createDto = ServiceTestDataBuilder.createPostDto("OrigTitle2", "Content", false, false,
                this.testUser.id,
                this.testCategory.id);
        final var created = this.postService.createPost(createDto);

        final var dto = ServiceTestDataBuilder.createPostDto("Title", "", false, false, this.testUser.id, null);
        assertThrows(ValidationException.class,
                () -> this.postService.updatePost(created.id, dto, this.testUser.username));
    }

    @Test
    @TestTransaction
    void testPatchPostNotFound() {
        final var dto = ServiceTestDataBuilder.createPostDto("Title", null, false, false, null, null);
        assertThrows(WebApplicationException.class, () -> this.postService.patchPost(999999L, dto, null));
    }

    @Test
    @TestTransaction
    void testPatchPostPublishedFlag() {
        final var createDto = ServiceTestDataBuilder.createPostDto("Patch Published", "Content", false, false,
                this.testUser.id,
                this.testCategory.id);
        final var created = this.postService.createPost(createDto);
        assertFalse(created.published);

        final var patchDto = new PostDto();
        patchDto.published = true;

        final var patched = this.postService.patchPost(created.id, patchDto, this.testUser.username);
        assertTrue(patched.published);
    }

    @Test
    @TestTransaction
    void testPatchPostCommentableFlag() {
        final var createDto = ServiceTestDataBuilder.createPostDto("Patch Commentable", "Content", false, false,
                this.testUser.id,
                this.testCategory.id);
        final var created = this.postService.createPost(createDto);
        assertFalse(created.commentable);

        final var patchDto = new PostDto();
        patchDto.commentable = true;

        final var patched = this.postService.patchPost(created.id, patchDto, this.testUser.username);
        assertTrue(patched.commentable);
    }

    @Test
    @TestTransaction
    void testCreatePostWithBlankTitle() {
        final var dto = ServiceTestDataBuilder.createPostDto("", "content", false, false, this.testUser.id,
                this.testCategory.id);
        assertThrows(ValidationException.class, () -> this.postService.createPost(dto));
    }

    @Test
    @TestTransaction
    void testCreatePostWithBlankContent() {
        final var dto = ServiceTestDataBuilder.createPostDto("Title", "", false, false, this.testUser.id,
                this.testCategory.id);
        assertThrows(ValidationException.class, () -> this.postService.createPost(dto));
    }

    @Test
    @TestTransaction
    void testCreatePostWithInvalidUserId() {
        final var dto = ServiceTestDataBuilder.createPostDto("Title", "Content", false, false, 999999L,
                this.testCategory.id);
        assertThrows(ValidationException.class, () -> this.postService.createPost(dto));
    }

    @Test
    @TestTransaction
    void testCreatePostWithInvalidCategoryId() {
        final var dto = ServiceTestDataBuilder.createPostDto("Title", "Content", false, false, this.testUser.id,
                999999L);
        assertThrows(ValidationException.class, () -> this.postService.createPost(dto));
    }

    @Test
    void testGetAllPostsWithPagination() {
        final var posts = this.postService.getAllPosts(0, 5);
        assertNotNull(posts);
    }

    @Test
    void testFindPublishedPostsWithPagination() {
        final var posts = this.postService.findPublishedPosts(0, 5);
        assertNotNull(posts);
    }
}
