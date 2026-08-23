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

    private String testUserPublicId;
    private String testCategoryPublicId;

    @SuppressWarnings("NullAway")
    @BeforeEach
    void setUp() {
        runCleanup(() -> {
            PostCommentEntity.deleteAll();
            cleanupTestData(PostEntity.class);
            this.cleanupTestUsers();

            this.testUser = setupTestUser(this.userService);
            this.testCategory = setupTestCategory();
            this.testUserPublicId = Objects.requireNonNull(this.testUser.publicId);
            this.testCategoryPublicId = Objects.requireNonNull(this.testCategory.publicId);
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
            assertTrue(Objects.requireNonNull(post.published));
        }
    }

    @Test
    @TestTransaction
    void findPublishedPosts_filtersByCategoryQueryDateAndPaginates() {
        final var username = Objects.requireNonNull(this.testUser.username);
        this.postService.createPost(ServiceTestDataBuilder.createPostDto("Alpha gardening tips", "content a", true,
                false, this.testCategoryPublicId), username);
        this.postService.createPost(ServiceTestDataBuilder.createPostDto("Beta cooking guide", "content b", true, false,
                this.testCategoryPublicId), username);
        // Unpublished draft must never be returned.
        this.postService.createPost(ServiceTestDataBuilder.createPostDto("Draft gardening", "content c", false, false,
                this.testCategoryPublicId), username);

        // Category filter returns only the two published posts in the test category.
        final var byCategory = this.postService.findPublishedPosts(this.testCategoryPublicId, null, null, null, 0, 20);
        assertEquals(2, byCategory.size());
        for (final var post : byCategory) {
            assertTrue(Objects.requireNonNull(post.published));
        }

        // Free-text query (scoped to the test category) matches the title.
        final var gardening =
                this.postService.findPublishedPosts(this.testCategoryPublicId, null, null, "gardening", 0, 20);
        assertEquals(1, gardening.size());
        assertEquals("Alpha gardening tips", gardening.get(0).title);

        // Pagination caps the page size.
        final var firstPage = this.postService.findPublishedPosts(this.testCategoryPublicId, null, null, null, 0, 1);
        assertEquals(1, firstPage.size());

        // A date range around today includes the posts; a past-only range excludes them.
        final var today = LocalDate.now(ZoneOffset.UTC);
        final var inRange = this.postService.findPublishedPosts(this.testCategoryPublicId, today.minusDays(1),
                today.plusDays(1), null, 0, 20);
        assertEquals(2, inRange.size());
        final var pastRange = this.postService.findPublishedPosts(this.testCategoryPublicId, LocalDate.of(2000, 1, 1),
                LocalDate.of(2000, 1, 2), null, 0, 20);
        assertTrue(pastRange.isEmpty());
    }

    @Test
    @TestTransaction
    void testCreatePost() {
        final var newPost = ServiceTestDataBuilder.createUniquePostDto(this.testCategoryPublicId);

        final var createdPost = this.postService.createPost(newPost, Objects.requireNonNull(this.testUser.username));

        assertNotNull(createdPost);
        assertNotNull(createdPost.publicId);
        assertEquals(newPost.title, createdPost.title);
        assertEquals(newPost.content, createdPost.content);
        assertNotNull(createdPost.created);
        assertNull(createdPost.lastEdit);
        assertFalse(Objects.requireNonNull(createdPost.published));
        assertFalse(Objects.requireNonNull(createdPost.commentable));
    }

    @Test
    @TestTransaction
    void testCreatePostWithPublishedFlag() {
        final var newPost = ServiceTestDataBuilder.createPostDto("Published Test Post", "Published test content", true,
                true, this.testCategoryPublicId);

        final var createdPost = this.postService.createPost(newPost, Objects.requireNonNull(this.testUser.username));

        assertNotNull(createdPost);
        assertTrue(Objects.requireNonNull(createdPost.published));
        assertTrue(Objects.requireNonNull(createdPost.commentable));
    }

    @Test
    @TestTransaction
    void testUpdatePost() {
        final var newPost = ServiceTestDataBuilder.createPostDto("Original Title", "Original content", false, false,
                this.testCategoryPublicId);
        final var createdPost = this.postService.createPost(newPost, Objects.requireNonNull(this.testUser.username));

        final var originalCreated = createdPost.created;

        final var updateDto =
                ServiceTestDataBuilder.createPostDto("Updated Title", "Updated content", false, false, null);

        final String postPublicId = Objects.requireNonNull(createdPost.publicId);
        final String username = Objects.requireNonNull(this.testUser.username);
        final var updatedPost = this.postService.updatePost(postPublicId, updateDto, username);

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
                this.testCategoryPublicId);
        final var createdPost = this.postService.createPost(newPost, Objects.requireNonNull(this.testUser.username));
        final var intruder = this.userService.createUser(ServiceTestDataBuilder.createUniqueUserDto());

        final var updateDto =
                ServiceTestDataBuilder.createPostDto("Blocked update", "Blocked content", false, false, null);

        final String postPublicId = Objects.requireNonNull(createdPost.publicId);
        final String intruderUsername = Objects.requireNonNull(intruder.username);
        final var exception = assertThrows(WebApplicationException.class,
                () -> this.postService.updatePost(postPublicId, updateDto, intruderUsername));
        assertEquals(403, exception.getResponse().getStatus());
    }

    @Test
    @TestTransaction
    void testPatchPost() {
        final var newPost = ServiceTestDataBuilder.createPostDto("Original Title", "Original content", false, false,
                this.testCategoryPublicId);
        final var createdPost = this.postService.createPost(newPost, Objects.requireNonNull(this.testUser.username));

        final var patchDto = ServiceTestDataBuilder.createPostDto("Patched Title", null, false, false, null);

        final String postPublicId = Objects.requireNonNull(createdPost.publicId);
        final String username = Objects.requireNonNull(this.testUser.username);
        final var patchedPost = this.postService.patchPost(postPublicId, patchDto, username);

        assertNotNull(patchedPost);
        assertEquals("Patched Title", patchedPost.title);
        assertEquals("Original content", patchedPost.content);
    }

    @Test
    @TestTransaction
    void testDeletePost() {
        final var newPost = ServiceTestDataBuilder.createPostDto("To Be Deleted", "This post will be deleted", false,
                false, this.testCategoryPublicId);
        final var createdPost = this.postService.createPost(newPost, Objects.requireNonNull(this.testUser.username));

        final String postPublicId = Objects.requireNonNull(createdPost.publicId);
        final String username = Objects.requireNonNull(this.testUser.username);

        final var deleted = this.postService.deletePost(postPublicId, username);

        assertTrue(deleted);
        final var deletedPost = this.postService.findById(postPublicId);
        assertTrue(deletedPost.isEmpty());
    }

    @Test
    void testDeleteNonExistentPost() {
        final var deleted = this.postService.deletePost("999999", null);
        assertFalse(deleted);
    }

    @Test
    @TestTransaction
    void testFindById() {
        final var newPost = ServiceTestDataBuilder.createPostDto("Find By ID Test", "Test content", false, false,
                this.testCategoryPublicId);
        final var createdPost = this.postService.createPost(newPost, Objects.requireNonNull(this.testUser.username));

        final String postPublicId = Objects.requireNonNull(createdPost.publicId);
        final var foundPost = this.postService.findById(postPublicId);

        assertTrue(foundPost.isPresent());
        assertEquals("Find By ID Test", foundPost.get().title);
    }

    @Test
    void testFindByIdNonExistent() {
        final var foundPost = this.postService.findById("999999");
        assertTrue(foundPost.isEmpty());
    }

    @Test
    @TestTransaction
    void testFindByUserId() {
        final var newPost = ServiceTestDataBuilder.createPostDto("User Post Test", "Test content", false, false,
                this.testCategoryPublicId);
        this.postService.createPost(newPost, Objects.requireNonNull(this.testUser.username));

        final var userPosts = this.postService.findByUserPublicId(this.testUserPublicId);

        assertNotNull(userPosts);
        assertFalse(userPosts.isEmpty());
        assertTrue(userPosts.stream().allMatch(post -> Objects.equals(post.userPublicId, this.testUserPublicId)));
    }

    @Test
    @TestTransaction
    void testFindByCategoryId() {
        final var newPost = ServiceTestDataBuilder.createPostDto("Category Post Test", "Test content", false, false,
                this.testCategoryPublicId);
        this.postService.createPost(newPost, Objects.requireNonNull(this.testUser.username));

        final var categoryPosts = this.postService.findByCategoryPublicId(this.testCategoryPublicId);

        assertNotNull(categoryPosts);
        assertFalse(categoryPosts.isEmpty());
        assertTrue(categoryPosts.stream()
                .allMatch(post -> Objects.equals(post.categoryPublicId, this.testCategoryPublicId)));
    }

    @Test
    void testSearchPostsWithNullOrBlankQueryUsesDefaultListing() {
        final var expected = this.postService.getAllPosts();

        final var nullResults = this.postService.searchPosts(null);
        final var blankResults = this.postService.searchPosts("   ");

        assertEquals(expected.stream().map(post -> post.publicId).toList(),
                nullResults.stream().map(post -> post.publicId).toList());
        assertEquals(expected.stream().map(post -> post.publicId).toList(),
                blankResults.stream().map(post -> post.publicId).toList());
    }

    @Test
    @TestTransaction
    void testSearchPostsWithQuery() {
        final var dto = ServiceTestDataBuilder.createPostDto("UniqueSearchTitle999", "content", false, false,
                this.testCategoryPublicId);
        this.postService.createPost(dto, Objects.requireNonNull(this.testUser.username));

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
                    this.testCategoryPublicId);
            this.postService.createPost(post, Objects.requireNonNull(this.testUser.username));
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
                this.testCategoryPublicId);
        this.postService.createPost(dto, Objects.requireNonNull(this.testUser.username));

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
        final var dto = ServiceTestDataBuilder.createPostDto("Title", "Content", false, false, null);
        assertThrows(WebApplicationException.class, () -> this.postService.updatePost("999999", dto, null));
    }

    @Test
    @TestTransaction
    void testUpdatePostWithBlankTitle() {
        final String postPublicId = createTestPostAndGetId();
        final String username = Objects.requireNonNull(this.testUser.username);
        final var dto = ServiceTestDataBuilder.createPostDto("", "Content", false, false, null);
        assertThrows(ValidationException.class, () -> this.postService.updatePost(postPublicId, dto, username));
    }

    @Test
    @TestTransaction
    void testUpdatePostWithBlankContent() {
        final String postPublicId = createTestPostAndGetId();
        final String username = Objects.requireNonNull(this.testUser.username);
        final var dto = ServiceTestDataBuilder.createPostDto("Title", "", false, false, null);
        assertThrows(ValidationException.class, () -> this.postService.updatePost(postPublicId, dto, username));
    }

    private String createTestPostAndGetId() {
        final var createDto = ServiceTestDataBuilder.createPostDto("SetupTitle", "SetupContent", false, false,
                this.testCategoryPublicId);
        final var created = this.postService.createPost(createDto, Objects.requireNonNull(this.testUser.username));
        return Objects.requireNonNull(created.publicId);
    }

    @Test
    @TestTransaction
    void testPatchPostNotFound() {
        final var dto = ServiceTestDataBuilder.createPostDto("Title", null, false, false, null);
        assertThrows(WebApplicationException.class, () -> this.postService.patchPost("999999", dto, null));
    }

    @Test
    @TestTransaction
    void testPatchPostPublishedFlag() {
        final var createDto = ServiceTestDataBuilder.createPostDto("Patch Published", "Content", false, false,
                this.testCategoryPublicId);
        final var created = this.postService.createPost(createDto, Objects.requireNonNull(this.testUser.username));
        assertFalse(Objects.requireNonNull(created.published));

        final var patchDto = new PostDto();
        patchDto.published = true;

        final String postPublicId = Objects.requireNonNull(created.publicId);
        final String username = Objects.requireNonNull(this.testUser.username);
        final var patched = this.postService.patchPost(postPublicId, patchDto, username);
        assertTrue(Objects.requireNonNull(patched.published));
    }

    @Test
    @TestTransaction
    void testPatchPostCommentableFlag() {
        final var createDto = ServiceTestDataBuilder.createPostDto("Patch Commentable", "Content", false, false,
                this.testCategoryPublicId);
        final var created = this.postService.createPost(createDto, Objects.requireNonNull(this.testUser.username));
        assertFalse(Objects.requireNonNull(created.commentable));

        final var patchDto = new PostDto();
        patchDto.commentable = true;

        final String postPublicId = Objects.requireNonNull(created.publicId);
        final String username = Objects.requireNonNull(this.testUser.username);
        final var patched = this.postService.patchPost(postPublicId, patchDto, username);
        assertTrue(Objects.requireNonNull(patched.commentable));
    }

    @Test
    @TestTransaction
    void testCreatePostWithBlankTitle() {
        final var dto = ServiceTestDataBuilder.createPostDto("", "content", false, false, this.testCategoryPublicId);
        assertThrows(ValidationException.class,
                () -> this.postService.createPost(dto, Objects.requireNonNull(this.testUser.username)));
    }

    @Test
    @TestTransaction
    void testCreatePostWithBlankContent() {
        final var dto = ServiceTestDataBuilder.createPostDto("Title", "", false, false, this.testCategoryPublicId);
        assertThrows(ValidationException.class,
                () -> this.postService.createPost(dto, Objects.requireNonNull(this.testUser.username)));
    }

    @Test
    @TestTransaction
    void testCreatePostWithInvalidUserId() {
        final var dto =
                ServiceTestDataBuilder.createPostDto("Title", "Content", false, false, this.testCategoryPublicId);
        assertThrows(ValidationException.class, () -> this.postService.createPost(dto, "999999"));
    }

    @Test
    @TestTransaction
    void testCreatePostWithInvalidCategoryId() {
        final var dto = ServiceTestDataBuilder.createPostDto("Title", "Content", false, false, "999999");
        assertThrows(ValidationException.class,
                () -> this.postService.createPost(dto, Objects.requireNonNull(this.testUser.username)));
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

    @Test
    @TestTransaction
    void testFindPublishedByDateRange() {
        final var dto = ServiceTestDataBuilder.createPostDto("Pub Date Range", "content", true, false,
                this.testCategoryPublicId);
        this.postService.createPost(dto, Objects.requireNonNull(this.testUser.username));

        final var results = this.postService.findPublishedByDateRange(LocalDate.now(ZoneOffset.UTC).minusDays(1),
                LocalDate.now(ZoneOffset.UTC).plusDays(1));
        assertNotNull(results);
        assertFalse(results.isEmpty());

        assertTrue(this.postService.findPublishedByDateRange(null, LocalDate.now(ZoneOffset.UTC)).isEmpty());
        assertTrue(this.postService.findPublishedByDateRange(LocalDate.now(ZoneOffset.UTC), null).isEmpty());
    }

    @Test
    @TestTransaction
    void testFindByUserAndCategoryPaginationAndPublished() {
        final var dto = ServiceTestDataBuilder.createPostDto("User Cat Pagination", "content", true, false,
                this.testCategoryPublicId);
        this.postService.createPost(dto, Objects.requireNonNull(this.testUser.username));

        assertFalse(this.postService.findByUserPublicId(this.testUserPublicId, 0, 5).isEmpty());
        assertFalse(this.postService.findPublishedByUserPublicId(this.testUserPublicId).isEmpty());
        assertFalse(this.postService.findPublishedByUserPublicId(this.testUserPublicId, 0, 5).isEmpty());
        assertFalse(this.postService.findByCategoryPublicId(this.testCategoryPublicId, 0, 5).isEmpty());
    }

    @Test
    @TestTransaction
    void testUpdateAndPatchWithCategoryChanges() {
        final var dto = ServiceTestDataBuilder.createPostDto("Cat Change Post", "content", false, false,
                this.testCategoryPublicId);
        final var created = this.postService.createPost(dto, Objects.requireNonNull(this.testUser.username));
        final String createdId = Objects.requireNonNull(created.publicId);
        final String username = Objects.requireNonNull(this.testUser.username);

        final var updateDto = ServiceTestDataBuilder.createPostDto("Updated Title", "content", false, false, "999999");
        assertThrows(ValidationException.class, () -> this.postService.updatePost(createdId, updateDto, username));

        final var patchDto = new PostDto();
        patchDto.categoryPublicId = "999999";
        assertThrows(ValidationException.class, () -> this.postService.patchPost(createdId, patchDto, username));

        final var clearCatDto = ServiceTestDataBuilder.createPostDto("Cleared Cat", "content", false, false, null);
        final var updated = this.postService.updatePost(createdId, clearCatDto, username);
        assertNull(updated.categoryPublicId);

        final var readdCatDto = new PostDto();
        readdCatDto.categoryPublicId = this.testCategoryPublicId;
        final var patched = this.postService.patchPost(createdId, readdCatDto, username);
        assertEquals(this.testCategoryPublicId, patched.categoryPublicId);
    }
}
