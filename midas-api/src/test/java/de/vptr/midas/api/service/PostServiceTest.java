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
import java.time.ZoneOffset;
import java.util.Objects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.vptr.midas.api.dto.PostDto;
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

    private long testUserId;
    private long testCategoryId;

    @SuppressWarnings("NullAway")
    @BeforeEach
    void setUp() {
        runCleanup(() -> {
            PostCommentEntity.deleteAll();
            cleanupTestData(PostEntity.class);
            this.cleanupTestUsers();

            this.testUser = setupTestUser(this.userService);
            this.testCategory = setupTestCategory();
            this.testUserId = Objects.requireNonNull(this.testUser.id);
            this.testCategoryId = Objects.requireNonNull(this.testCategory.id);
        });
    }

    private void cleanupTestUsers() {
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

        for (final var post : publishedPosts) {
            assertTrue(Boolean.TRUE.equals(post.published));
        }
    }

    @Test
    @TestTransaction
    void testCreatePost() {
        final var newPost = ServiceTestDataBuilder.createUniquePostDto(this.testUserId, this.testCategoryId);

        final var createdPost = this.postService.createPost(newPost);

        assertNotNull(createdPost);
        assertNotNull(createdPost.id);
        assertEquals(newPost.title, createdPost.title);
        assertEquals(newPost.content, createdPost.content);
        assertNotNull(createdPost.created);
        assertNull(createdPost.lastEdit);
        assertFalse(Boolean.TRUE.equals(createdPost.published));
        assertFalse(Boolean.TRUE.equals(createdPost.commentable));
    }

    @Test
    @TestTransaction
    void testCreatePostWithPublishedFlag() {
        final var newPost = ServiceTestDataBuilder.createPostDto("Published Test Post", "Published test content", true,
                true, this.testUserId, this.testCategoryId);

        final var createdPost = this.postService.createPost(newPost);

        assertNotNull(createdPost);
        assertTrue(Boolean.TRUE.equals(createdPost.published));
        assertTrue(Boolean.TRUE.equals(createdPost.commentable));
    }

    @Test
    @TestTransaction
    void testUpdatePost() {
        final var newPost = ServiceTestDataBuilder.createPostDto("Original Title", "Original content", false, false,
                this.testUserId, this.testCategoryId);
        final var createdPost = this.postService.createPost(newPost);

        final var originalCreated = createdPost.created;

        final var updateDto =
                ServiceTestDataBuilder.createPostDto("Updated Title", "Updated content", false, false, null, null);

        final Long postId = Objects.requireNonNull(createdPost.id);
        final String username = Objects.requireNonNull(this.testUser.username);
        final var updatedPost = this.postService.updatePost(postId, updateDto, username);

        assertNotNull(updatedPost);
        assertEquals("Updated Title", updatedPost.title);
        assertEquals("Updated content", updatedPost.content);
        assertEquals(originalCreated, updatedPost.created);
        assertNotNull(updatedPost.lastEdit);
    }

    @Test
    @TestTransaction
    void testUpdatePostShouldDenyNonOwnerWithoutAdminAccess() {
        final var newPost = ServiceTestDataBuilder.createPostDto("Ownership Title", "Ownership content", false, false,
                this.testUserId, this.testCategoryId);
        final var createdPost = this.postService.createPost(newPost);
        final var intruder = this.userService.createUser(ServiceTestDataBuilder.createUniqueUserDto());

        final var updateDto =
                ServiceTestDataBuilder.createPostDto("Blocked update", "Blocked content", false, false, null, null);

        final Long postId = Objects.requireNonNull(createdPost.id);
        final String intruderUsername = Objects.requireNonNull(intruder.username);
        final var exception = assertThrows(WebApplicationException.class,
                () -> this.postService.updatePost(postId, updateDto, intruderUsername));
        assertEquals(403, exception.getResponse().getStatus());
    }

    @Test
    @TestTransaction
    void testPatchPost() {
        final var newPost = ServiceTestDataBuilder.createPostDto("Original Title", "Original content", false, false,
                this.testUserId, this.testCategoryId);
        final var createdPost = this.postService.createPost(newPost);

        final var patchDto = ServiceTestDataBuilder.createPostDto("Patched Title", null, false, false, null, null);

        final Long postId = Objects.requireNonNull(createdPost.id);
        final String username = Objects.requireNonNull(this.testUser.username);
        final var patchedPost = this.postService.patchPost(postId, patchDto, username);

        assertNotNull(patchedPost);
        assertEquals("Patched Title", patchedPost.title);
        assertEquals("Original content", patchedPost.content);
    }

    @Test
    @TestTransaction
    void testDeletePost() {
        final var newPost = ServiceTestDataBuilder.createPostDto("To Be Deleted", "This post will be deleted", false,
                false, this.testUserId, this.testCategoryId);
        final var createdPost = this.postService.createPost(newPost);

        final Long postId = Objects.requireNonNull(createdPost.id);
        final String username = Objects.requireNonNull(this.testUser.username);

        final var deleted = this.postService.deletePost(postId, username);

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
        final var newPost = ServiceTestDataBuilder.createPostDto("Find By ID Test", "Test content", false, false,
                this.testUserId, this.testCategoryId);
        final var createdPost = this.postService.createPost(newPost);

        final Long postId = Objects.requireNonNull(createdPost.id);
        final var foundPost = this.postService.findById(postId);

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
        final var newPost = ServiceTestDataBuilder.createPostDto("User Post Test", "Test content", false, false,
                this.testUserId, this.testCategoryId);
        this.postService.createPost(newPost);

        final var userPosts = this.postService.findByUserId(this.testUserId);

        assertNotNull(userPosts);
        assertFalse(userPosts.isEmpty());
        assertTrue(userPosts.stream().allMatch(post -> Objects.equals(post.userId, this.testUserId)));
    }

    @Test
    @TestTransaction
    void testFindByCategoryId() {
        final var newPost = ServiceTestDataBuilder.createPostDto("Category Post Test", "Test content", false, false,
                this.testUserId, this.testCategoryId);
        this.postService.createPost(newPost);

        final var categoryPosts = this.postService.findByCategoryId(this.testCategoryId);

        assertNotNull(categoryPosts);
        assertFalse(categoryPosts.isEmpty());
        assertTrue(categoryPosts.stream().allMatch(post -> Objects.equals(post.categoryId, this.testCategoryId)));
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
                this.testUserId, this.testCategoryId);
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
            final var post = ServiceTestDataBuilder.createPostDto(token + "-" + i, "content-" + i, false, false,
                    this.testUserId, this.testCategoryId);
            this.postService.createPost(post);
        }

        final var results = this.postService.searchPosts(token);

        assertEquals(PaginationUtil.MAX_SIZE, results.size());
    }

    @Test
    void testFindByDateRangeWithNullStart() {
        final var results = this.postService.findByDateRange(null, LocalDate.of(2025, 1, 31));
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testFindByDateRangeWithNullEnd() {
        final var results = this.postService.findByDateRange(LocalDate.of(2025, 1, 1), null);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @TestTransaction
    void testFindByDateRangeValid() {
        final var dto = ServiceTestDataBuilder.createPostDto("Date Range Test", "content", false, false,
                this.testUserId, this.testCategoryId);
        this.postService.createPost(dto);

        final var results = this.postService.findByDateRange(LocalDate.now(ZoneOffset.UTC).minusDays(1),
                LocalDate.now(ZoneOffset.UTC).plusDays(1));
        assertNotNull(results);
        assertFalse(results.isEmpty());
    }

    @Test
    void testFindByDateRangeNull() {
        final var results = this.postService.findByDateRange(null, null);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @TestTransaction
    void testUpdatePostNotFound() {
        final var dto = ServiceTestDataBuilder.createPostDto("Title", "Content", false, false, this.testUserId, null);
        assertThrows(WebApplicationException.class, () -> this.postService.updatePost(999999L, dto, null));
    }

    @Test
    @TestTransaction
    void testUpdatePostWithBlankTitle() {
        final var createDto = ServiceTestDataBuilder.createPostDto("OrigTitle", "Content", false, false,
                this.testUserId, this.testCategoryId);
        final var created = this.postService.createPost(createDto);

        final Long postId = Objects.requireNonNull(created.id);
        final String username = Objects.requireNonNull(this.testUser.username);
        final var dto = ServiceTestDataBuilder.createPostDto("", "Content", false, false, this.testUserId, null);
        assertThrows(ValidationException.class, () -> this.postService.updatePost(postId, dto, username));
    }

    @Test
    @TestTransaction
    void testUpdatePostWithBlankContent() {
        final var createDto = ServiceTestDataBuilder.createPostDto("OrigTitle2", "Content", false, false,
                this.testUserId, this.testCategoryId);
        final var created = this.postService.createPost(createDto);

        final Long postId = Objects.requireNonNull(created.id);
        final String username = Objects.requireNonNull(this.testUser.username);
        final var dto = ServiceTestDataBuilder.createPostDto("Title", "", false, false, this.testUserId, null);
        assertThrows(ValidationException.class, () -> this.postService.updatePost(postId, dto, username));
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
                this.testUserId, this.testCategoryId);
        final var created = this.postService.createPost(createDto);
        assertFalse(Boolean.TRUE.equals(created.published));

        final var patchDto = new PostDto();
        patchDto.published = true;

        final Long postId = Objects.requireNonNull(created.id);
        final String username = Objects.requireNonNull(this.testUser.username);
        final var patched = this.postService.patchPost(postId, patchDto, username);
        assertTrue(Boolean.TRUE.equals(patched.published));
    }

    @Test
    @TestTransaction
    void testPatchPostCommentableFlag() {
        final var createDto = ServiceTestDataBuilder.createPostDto("Patch Commentable", "Content", false, false,
                this.testUserId, this.testCategoryId);
        final var created = this.postService.createPost(createDto);
        assertFalse(Boolean.TRUE.equals(created.commentable));

        final var patchDto = new PostDto();
        patchDto.commentable = true;

        final Long postId = Objects.requireNonNull(created.id);
        final String username = Objects.requireNonNull(this.testUser.username);
        final var patched = this.postService.patchPost(postId, patchDto, username);
        assertTrue(Boolean.TRUE.equals(patched.commentable));
    }

    @Test
    @TestTransaction
    void testCreatePostWithBlankTitle() {
        final var dto =
                ServiceTestDataBuilder.createPostDto("", "content", false, false, this.testUserId, this.testCategoryId);
        assertThrows(ValidationException.class, () -> this.postService.createPost(dto));
    }

    @Test
    @TestTransaction
    void testCreatePostWithBlankContent() {
        final var dto =
                ServiceTestDataBuilder.createPostDto("Title", "", false, false, this.testUserId, this.testCategoryId);
        assertThrows(ValidationException.class, () -> this.postService.createPost(dto));
    }

    @Test
    @TestTransaction
    void testCreatePostWithInvalidUserId() {
        final var dto =
                ServiceTestDataBuilder.createPostDto("Title", "Content", false, false, 999999L, this.testCategoryId);
        assertThrows(ValidationException.class, () -> this.postService.createPost(dto));
    }

    @Test
    @TestTransaction
    void testCreatePostWithInvalidCategoryId() {
        final var dto =
                ServiceTestDataBuilder.createPostDto("Title", "Content", false, false, this.testUserId, 999999L);
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
