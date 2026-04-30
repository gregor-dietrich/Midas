package de.vptr.midas.api.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.vptr.midas.api.dto.PostDto;
import de.vptr.midas.api.dto.PostResponseDto;
import de.vptr.midas.api.entity.PostCategoryEntity;
import de.vptr.midas.api.entity.PostCommentEntity;
import de.vptr.midas.api.entity.PostEntity;
import de.vptr.midas.api.entity.UserEntity;
import de.vptr.midas.api.util.SearchQueryUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/** Service for post business logic. */
@ApplicationScoped
public class PostService {

    @Inject
    AdminAccessService adminAccessService;

    private void verifyPostOwnership(final String currentUsername, final PostEntity post) {
        if (currentUsername == null) {
            throw new ValidationException("Authentication required to modify posts.");
        }
        if (post.user != null && !post.user.username.equals(currentUsername)
                && !this.adminAccessService.hasAdminViewAccess(currentUsername)) {
            throw new WebApplicationException("Access denied to edit post", Response.Status.FORBIDDEN);
        }
    }

    /** Returns all posts with default pagination. */
    public List<PostResponseDto> getAllPosts() {
        return getAllPosts(0, 20);
    }

    /** Returns all posts with the given pagination. */
    public List<PostResponseDto> getAllPosts(final int page, final int size) {
        final List<PostEntity> posts = PostEntity
                .find("SELECT p FROM PostEntity p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.category ORDER BY p.created DESC")
                .page(page, size)
                .list();
        return this.toResponseDtos(posts);
    }

    /** Returns the post with the given id. */
    public Optional<PostResponseDto> findById(final Long id) {
        return PostEntity
                .find("SELECT p FROM PostEntity p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.category WHERE p.id = ?1",
                        id)
                .<PostEntity>firstResultOptional()
                .map(this::toResponseDto);
    }

    /** Returns all published posts with default pagination. */
    public List<PostResponseDto> findPublishedPosts() {
        return findPublishedPosts(0, 20);
    }

    /** Returns all published posts with the given pagination. */
    public List<PostResponseDto> findPublishedPosts(final int page, final int size) {
        final List<PostEntity> posts = PostEntity
                .find("SELECT p FROM PostEntity p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.category WHERE p.published = true ORDER BY p.created DESC")
                .page(page, size)
                .list();
        return this.toResponseDtos(posts);
    }

    /** Returns all posts by the given user. */
    /** Returns all posts by the given user with default pagination. */
    public List<PostResponseDto> findByUserId(final Long userId) {
        return findByUserId(userId, 0, 20);
    }

    /** Returns all posts by the given user with pagination. */
    public List<PostResponseDto> findByUserId(final Long userId, final int page, final int size) {
        final List<PostEntity> posts = PostEntity
                .find("SELECT p FROM PostEntity p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.category WHERE p.user.id = ?1 ORDER BY p.created DESC",
                        userId)
                .page(page, size).list();
        return this.toResponseDtos(posts);
    }

    /** Returns published posts by the given user. */
    public List<PostResponseDto> findPublishedByUserId(final Long userId) {
        return findPublishedByUserId(userId, 0, 20);
    }

    /** Returns published posts by the given user with pagination. */
    public List<PostResponseDto> findPublishedByUserId(final Long userId, final int page, final int size) {
        final List<PostEntity> posts = PostEntity
                .find("SELECT p FROM PostEntity p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.category "
                        + "WHERE p.user.id = ?1 AND p.published = true ORDER BY p.created DESC",
                        userId)
                .page(page, size).list();
        return this.toResponseDtos(posts);
    }

    /** Returns all posts in the given category with default pagination. */
    public List<PostResponseDto> findByCategoryId(final Long categoryId) {
        return findByCategoryId(categoryId, 0, 20);
    }

    /** Returns all posts in the given category with pagination. */
    public List<PostResponseDto> findByCategoryId(final Long categoryId, final int page, final int size) {
        final List<PostEntity> posts = PostEntity
                .find("SELECT p FROM PostEntity p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.category WHERE p.category.id = ?1 ORDER BY p.created DESC",
                        categoryId)
                .page(page, size).list();
        return this.toResponseDtos(posts);
    }

    /** Creates a new post. */
    @Transactional
    public PostResponseDto createPost(final PostDto postDto) {
        // Validate required fields for POST
        if (postDto.title == null || postDto.title.trim().isEmpty()) {
            throw new ValidationException("Title is required for creating a post");
        }
        if (postDto.content == null || postDto.content.trim().isEmpty()) {
            throw new ValidationException("Content is required for creating a post");
        }
        if (postDto.userId == null) {
            throw new ValidationException("User ID is required for creating a post");
        }

        final PostEntity post = this.buildPostEntity(postDto);
        post.persistAndFlush();
        PostEntity.getEntityManager().detach(post);
        return this.toResponseDto(PostEntity.findById(post.id));
    }

    @SuppressFBWarnings(value = "UC_USELESS_OBJECT", justification = "Entity populated then persisted via Panache; SpotBugs cannot see Panache's bytecode-generated persist()")
    private PostEntity buildPostEntity(final PostDto postDto) {
        final PostEntity post = new PostEntity();
        post.title = postDto.title;
        post.content = postDto.content;
        post.published = postDto.published != null ? postDto.published : false;
        post.commentable = postDto.commentable != null ? postDto.commentable : false;

        // Set user - required for creation
        final UserEntity user = UserEntity.findById(postDto.userId);
        if (user == null) {
            throw new ValidationException("User with ID " + postDto.userId + " not found");
        }
        post.user = user;

        // Set category if provided
        if (postDto.categoryId != null) {
            final PostCategoryEntity category = PostCategoryEntity.findById(postDto.categoryId);
            if (category == null) {
                throw new ValidationException("Category with ID " + postDto.categoryId + " not found");
            }
            post.category = category;
        }

        return post;
    }

    /** Updates the post with the given id. */
    @Transactional
    public PostResponseDto updatePost(final Long id, final PostDto postDto, final String currentUsername) {
        // Validate required fields for PUT
        if (postDto.title == null || postDto.title.trim().isEmpty()) {
            throw new ValidationException("Title is required for updating a post");
        }
        if (postDto.content == null || postDto.content.trim().isEmpty()) {
            throw new ValidationException("Content is required for updating a post");
        }

        final PostEntity existingPost = PostEntity.findById(id);
        if (existingPost == null) {
            throw new WebApplicationException("Post not found", Response.Status.NOT_FOUND);
        }

        this.verifyPostOwnership(currentUsername, existingPost);

        // Complete replacement (PUT semantics)
        existingPost.title = postDto.title;
        existingPost.content = postDto.content;
        existingPost.published = postDto.published != null ? postDto.published : false;
        existingPost.commentable = postDto.commentable != null ? postDto.commentable : false;

        // userId changes are not allowed — preserve existing user

        // Set category if provided
        if (postDto.categoryId != null) {
            final PostCategoryEntity category = PostCategoryEntity.findById(postDto.categoryId);
            if (category == null) {
                throw new ValidationException("Category with ID " + postDto.categoryId + " not found");
            }
            existingPost.category = category;
        } else {
            existingPost.category = null;
        }

        existingPost.persistAndFlush();
        PostEntity.getEntityManager().detach(existingPost);
        return this.toResponseDto(PostEntity.findById(existingPost.id));
    }

    /** Partially updates the post with the given id. */
    @Transactional
    public PostResponseDto patchPost(final Long id, final PostDto postDto, final String currentUsername) {
        final PostEntity existingPost = PostEntity.findById(id);
        if (existingPost == null) {
            throw new WebApplicationException("Post not found", Response.Status.NOT_FOUND);
        }

        this.verifyPostOwnership(currentUsername, existingPost);

        // Partial update (PATCH semantics) - only update provided fields
        if (postDto.title != null && !postDto.title.trim().isEmpty()) {
            existingPost.title = postDto.title;
        }
        if (postDto.content != null && !postDto.content.trim().isEmpty()) {
            existingPost.content = postDto.content;
        }
        if (postDto.published != null) {
            existingPost.published = postDto.published;
        }
        if (postDto.commentable != null) {
            existingPost.commentable = postDto.commentable;
        }

        // userId changes are not allowed — preserve existing user

        // Set category if provided
        if (postDto.categoryId != null) {
            final PostCategoryEntity category = PostCategoryEntity.findById(postDto.categoryId);
            if (category == null) {
                throw new ValidationException("Category with ID " + postDto.categoryId + " not found");
            }
            existingPost.category = category;
        }

        existingPost.persistAndFlush();
        PostEntity.getEntityManager().detach(existingPost);
        return this.toResponseDto(PostEntity.findById(existingPost.id));
    }

    /** Deletes the post with the given id. */
    @Transactional
    public boolean deletePost(final Long id, final String currentUsername) {
        final PostEntity existingPost = PostEntity.findById(id);
        if (existingPost == null) {
            return false;
        }

        this.verifyPostOwnership(currentUsername, existingPost);

        return PostEntity.deleteById(id);
    }

    /** Returns all posts matching the given query. */
    public List<PostResponseDto> searchPosts(final String query) {
        final String searchTerm = SearchQueryUtil.toContainsLikePattern(query);
        if (searchTerm == null) {
            return this.getAllPosts();
        }
        final List<PostEntity> posts = SearchQueryUtil.applyDefaultSearchLimit(PostEntity.find(
                "SELECT p FROM PostEntity p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.category "
                        + "WHERE LOWER(p.title) LIKE ?1 ESCAPE '\\' "
                        + "OR LOWER(p.content) LIKE ?1 ESCAPE '\\' "
                        + "OR LOWER(p.user.username) LIKE ?1 ESCAPE '\\' "
                        + "ORDER BY p.created DESC",
                searchTerm)).list();
        return this.toResponseDtos(posts);
    }

    /** Returns posts within the given date range. */
    public List<PostResponseDto> findByDateRange(final String startDate, final String endDate) {
        if (startDate == null || endDate == null) {
            return this.getAllPosts();
        }

        try {
            final LocalDate start = LocalDate.parse(startDate);
            final LocalDate end = LocalDate.parse(endDate);

            final LocalDateTime startDateTime = start.atStartOfDay();
            final LocalDateTime endDateTime = end.atTime(LocalTime.MAX);

            final List<PostEntity> posts = PostEntity
                    .find("SELECT p FROM PostEntity p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.category "
                            + "WHERE p.created >= ?1 AND p.created <= ?2 ORDER BY p.created DESC",
                            startDateTime, endDateTime)
                    .list();
            return this.toResponseDtos(posts);
        } catch (final DateTimeParseException e) {
            throw new ValidationException("Invalid date format. Expected: yyyy-MM-dd");
        }
    }

    /** Returns published posts within the given date range. */
    public List<PostResponseDto> findPublishedByDateRange(final String startDate, final String endDate) {
        if (startDate == null || endDate == null) {
            return this.findPublishedPosts();
        }

        try {
            final LocalDate start = LocalDate.parse(startDate);
            final LocalDate end = LocalDate.parse(endDate);

            final LocalDateTime startDateTime = start.atStartOfDay();
            final LocalDateTime endDateTime = end.atTime(LocalTime.MAX);

            final List<PostEntity> posts = PostEntity
                    .find("SELECT p FROM PostEntity p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.category "
                            + "WHERE p.created >= ?1 AND p.created <= ?2 AND p.published = true "
                            + "ORDER BY p.created DESC",
                            startDateTime, endDateTime)
                    .list();
            return this.toResponseDtos(posts);
        } catch (final DateTimeParseException e) {
            throw new ValidationException("Invalid date format. Expected: yyyy-MM-dd");
        }
    }

    private PostResponseDto toResponseDto(final PostEntity post) {
        final long commentsCount = PostCommentEntity.count("post.id", post.id);
        return new PostResponseDto(post, commentsCount);
    }

    private List<PostResponseDto> toResponseDtos(final List<PostEntity> posts) {
        if (posts.isEmpty()) {
            return List.of();
        }
        final List<Long> ids = posts.stream().map(p -> p.id).toList();

        final List<Object[]> countResults = PostCommentEntity.getEntityManager()
                .createQuery(
                        "SELECT c.post.id, COUNT(c) FROM PostCommentEntity c WHERE c.post.id IN :ids GROUP BY c.post.id",
                        Object[].class)
                .setParameter("ids", ids)
                .getResultList();
        final Map<Long, Long> commentCounts = new HashMap<>();
        for (final Object[] row : countResults) {
            commentCounts.put((Long) row[0], (Long) row[1]);
        }

        return posts.stream()
                .map(p -> new PostResponseDto(p, commentCounts.getOrDefault(p.id, 0L)))
                .toList();
    }
}
