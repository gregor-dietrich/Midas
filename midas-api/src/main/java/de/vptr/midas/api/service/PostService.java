package de.vptr.midas.api.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
import de.vptr.midas.api.security.HtmlSanitizerService;
import de.vptr.midas.api.util.SearchQueryUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;
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

    @Inject
    HtmlSanitizerService htmlSanitizer;

    private void verifyPostOwnership(@Nullable final String currentUsername, final PostEntity post) {
        if (currentUsername == null) {
            throw new ValidationException("Authentication required to modify posts.");
        }
        if (post.user != null && !currentUsername.equals(post.user.username)
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
        final List<PostEntity> posts = PostEntity.find(
                "SELECT p FROM PostEntity p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.category ORDER BY p.created DESC")
                .page(page, size).list();
        return this.toResponseDtos(posts);
    }

    /** Returns the post with the given publicId. */
    public Optional<PostResponseDto> findById(final String publicId) {
        return PostEntity.find(
                "SELECT p FROM PostEntity p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.category WHERE p.publicId = ?1",
                publicId).<PostEntity>firstResultOptional().map(this::toResponseDto);
    }

    /** Returns all published posts with default pagination. */
    public List<PostResponseDto> findPublishedPosts() {
        return findPublishedPosts(0, 20);
    }

    /** Returns all published posts with the given pagination. */
    public List<PostResponseDto> findPublishedPosts(final int page, final int size) {
        final List<PostEntity> posts = PostEntity.find(
                "SELECT p FROM PostEntity p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.category WHERE p.published = true ORDER BY p.created DESC")
                .page(page, size).list();
        return this.toResponseDtos(posts);
    }

    /**
     * Returns published posts filtered server-side by optional category, created-date range (inclusive of the whole end
     * day) and free-text query, with pagination. Mirrors the payments filtering approach so the GUI never loads the
     * full post set into memory.
     */
    public List<PostResponseDto> findPublishedPosts(@Nullable final String categoryPublicId,
            @Nullable final LocalDate startDate, @Nullable final LocalDate endDate, @Nullable final String query,
            final int page, final int size) {
        final LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        final LocalDateTime endDateTime = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;
        final String like = SearchQueryUtil.toContainsLikePattern(query);

        final StringBuilder jpql = new StringBuilder("SELECT p FROM PostEntity p "
                + "LEFT JOIN FETCH p.user LEFT JOIN FETCH p.category WHERE p.published = true");
        if (categoryPublicId != null) {
            jpql.append(" AND p.category.publicId = :categoryPublicId");
        }
        if (startDateTime != null) {
            jpql.append(" AND p.created >= :startDateTime");
        }
        if (endDateTime != null) {
            jpql.append(" AND p.created <= :endDateTime");
        }
        if (like != null) {
            jpql.append(" AND (LOWER(p.title) LIKE :like ESCAPE '\\'")
                    .append(" OR LOWER(p.content) LIKE :like ESCAPE '\\'")
                    .append(" OR LOWER(p.user.username) LIKE :like ESCAPE '\\')");
        }
        jpql.append(" ORDER BY p.created DESC");

        final var jpaQuery = PostEntity.getEntityManager().createQuery(jpql.toString(), PostEntity.class);
        if (categoryPublicId != null) {
            jpaQuery.setParameter("categoryPublicId", categoryPublicId);
        }
        if (startDateTime != null) {
            jpaQuery.setParameter("startDateTime", startDateTime);
        }
        if (endDateTime != null) {
            jpaQuery.setParameter("endDateTime", endDateTime);
        }
        if (like != null) {
            jpaQuery.setParameter("like", like);
        }
        jpaQuery.setFirstResult(page * size).setMaxResults(size);

        return this.toResponseDtos(jpaQuery.getResultList());
    }

    /** Returns all posts by the given user with default pagination. */
    public List<PostResponseDto> findByUserPublicId(final String userPublicId) {
        return findByUserPublicId(userPublicId, 0, 20);
    }

    /** Returns all posts by the given user with pagination. */
    public List<PostResponseDto> findByUserPublicId(final String userPublicId, final int page, final int size) {
        final List<PostEntity> posts = PostEntity.find(
                "SELECT p FROM PostEntity p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.category WHERE p.user.publicId = ?1 ORDER BY p.created DESC",
                userPublicId).page(page, size).list();
        return this.toResponseDtos(posts);
    }

    /** Returns published posts by the given user. */
    public List<PostResponseDto> findPublishedByUserPublicId(final String userPublicId) {
        return findPublishedByUserPublicId(userPublicId, 0, 20);
    }

    /** Returns published posts by the given user with pagination. */
    public List<PostResponseDto> findPublishedByUserPublicId(final String userPublicId, final int page,
            final int size) {
        final List<PostEntity> posts = PostEntity.find("""
                SELECT p FROM PostEntity p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.category \
                WHERE p.user.publicId = ?1 AND p.published = true ORDER BY p.created DESC""", userPublicId)
                .page(page, size).list();
        return this.toResponseDtos(posts);
    }

    /** Returns all posts in the given category with default pagination. */
    public List<PostResponseDto> findByCategoryPublicId(final String categoryPublicId) {
        return findByCategoryPublicId(categoryPublicId, 0, 20);
    }

    /** Returns all posts in the given category with pagination. */
    public List<PostResponseDto> findByCategoryPublicId(final String categoryPublicId, final int page, final int size) {
        final List<PostEntity> posts = PostEntity.find(
                "SELECT p FROM PostEntity p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.category WHERE p.category.publicId = ?1 ORDER BY p.created DESC",
                categoryPublicId).page(page, size).list();
        return this.toResponseDtos(posts);
    }

    /** Creates a new post. */
    @Transactional
    public PostResponseDto createPost(final PostDto postDto, final String currentUsername) {
        final PostEntity post = this.buildPostEntity(postDto, currentUsername);
        post.persistAndFlush();
        return this.toResponseDto(post);
    }

    @SuppressFBWarnings(value = "UC_USELESS_OBJECT", justification = "Entity populated then persisted via Panache;"
            + " SpotBugs cannot see Panache's bytecode-generated persist()")
    private PostEntity buildPostEntity(final PostDto postDto, final String currentUsername) {
        final PostEntity post = new PostEntity();
        post.title = postDto.title;
        post.content = this.htmlSanitizer.sanitizeRichContent(postDto.content);
        post.published = postDto.published != null ? postDto.published : false;
        post.commentable = postDto.commentable != null ? postDto.commentable : false;

        // Set user - required for creation
        final UserEntity user = UserEntity.find("username", currentUsername).firstResult();
        if (user == null) {
            throw new ValidationException("Authenticated user " + currentUsername + " not found");
        }
        post.user = user;

        // Set category if provided
        if (postDto.categoryPublicId != null) {
            final PostCategoryEntity category =
                    PostCategoryEntity.find("publicId", postDto.categoryPublicId).firstResult();
            if (category == null) {
                throw new ValidationException("Category with Public ID " + postDto.categoryPublicId + " not found");
            }
            post.category = category;
        }

        return post;
    }

    /** Updates the post with the given publicId. */
    @Transactional
    public PostResponseDto updatePost(final String publicId, final PostDto postDto,
            @Nullable final String currentUsername) {
        final PostEntity existingPost = PostEntity.find("publicId", publicId).firstResult();
        if (existingPost == null) {
            throw new WebApplicationException("Post not found", Response.Status.NOT_FOUND);
        }

        this.verifyPostOwnership(currentUsername, existingPost);

        // Complete replacement (PUT semantics)
        existingPost.title = postDto.title;
        existingPost.content = this.htmlSanitizer.sanitizeRichContent(postDto.content);
        existingPost.published = postDto.published != null ? postDto.published : false;
        existingPost.commentable = postDto.commentable != null ? postDto.commentable : false;

        // userPublicId changes are not allowed — preserve existing user

        // Set category if provided
        if (postDto.categoryPublicId != null) {
            final PostCategoryEntity category =
                    PostCategoryEntity.find("publicId", postDto.categoryPublicId).firstResult();
            if (category == null) {
                throw new ValidationException("Category with Public ID " + postDto.categoryPublicId + " not found");
            }
            existingPost.category = category;
        } else {
            existingPost.category = null;
        }

        existingPost.persistAndFlush();
        return this.toResponseDto(existingPost);
    }

    /** Partially updates the post with the given publicId. */
    @Transactional
    public PostResponseDto patchPost(final String publicId, final PostDto postDto,
            @Nullable final String currentUsername) {
        final PostEntity existingPost = PostEntity.find("publicId", publicId).firstResult();
        if (existingPost == null) {
            throw new WebApplicationException("Post not found", Response.Status.NOT_FOUND);
        }

        this.verifyPostOwnership(currentUsername, existingPost);

        // Partial update (PATCH semantics) - only update provided fields
        if (postDto.title != null && !postDto.title.trim().isEmpty()) {
            existingPost.title = postDto.title;
        }
        if (postDto.content != null && !postDto.content.trim().isEmpty()) {
            existingPost.content = this.htmlSanitizer.sanitizeRichContent(postDto.content);
        }
        if (postDto.published != null) {
            existingPost.published = postDto.published;
        }
        if (postDto.commentable != null) {
            existingPost.commentable = postDto.commentable;
        }

        // userPublicId changes are not allowed — preserve existing user

        // Set category if provided
        if (postDto.categoryPublicId != null) {
            final PostCategoryEntity category =
                    PostCategoryEntity.find("publicId", postDto.categoryPublicId).firstResult();
            if (category == null) {
                throw new ValidationException("Category with Public ID " + postDto.categoryPublicId + " not found");
            }
            existingPost.category = category;
        }

        existingPost.persistAndFlush();
        return this.toResponseDto(existingPost);
    }

    /** Deletes the post with the given publicId. */
    @Transactional
    public boolean deletePost(final String publicId, @Nullable final String currentUsername) {
        final PostEntity existingPost = PostEntity.find("publicId", publicId).firstResult();
        if (existingPost == null) {
            return false;
        }

        this.verifyPostOwnership(currentUsername, existingPost);

        return PostEntity.delete("publicId", publicId) > 0;
    }

    /** Returns all posts matching the given query. */
    public List<PostResponseDto> searchPosts(@Nullable final String query) {
        final String searchTerm = SearchQueryUtil.toContainsLikePattern(query);
        if (searchTerm == null) {
            return this.getAllPosts();
        }
        final List<PostEntity> posts = SearchQueryUtil.applyDefaultSearchLimit(PostEntity.find("""
                SELECT p FROM PostEntity p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.category \
                WHERE LOWER(p.title) LIKE ?1 ESCAPE '\\' \
                OR LOWER(p.content) LIKE ?1 ESCAPE '\\' \
                OR LOWER(p.user.username) LIKE ?1 ESCAPE '\\' \
                ORDER BY p.created DESC""", searchTerm)).list();
        return this.toResponseDtos(posts);
    }

    /** Returns posts within the given date range. */
    public List<PostResponseDto> findByDateRange(@Nullable final LocalDate startDate,
            @Nullable final LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return List.of();
        }

        final LocalDateTime startDateTime = startDate.atStartOfDay();
        final LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        final List<PostEntity> posts = PostEntity.find("""
                SELECT p FROM PostEntity p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.category \
                WHERE p.created >= ?1 AND p.created <= ?2 ORDER BY p.created DESC""", startDateTime, endDateTime)
                .list();
        return this.toResponseDtos(posts);
    }

    /** Returns published posts within the given date range. */
    public List<PostResponseDto> findPublishedByDateRange(@Nullable final LocalDate startDate,
            @Nullable final LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return List.of();
        }

        final LocalDateTime startDateTime = startDate.atStartOfDay();
        final LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        final List<PostEntity> posts = PostEntity.find("""
                SELECT p FROM PostEntity p LEFT JOIN FETCH p.user LEFT JOIN FETCH p.category \
                WHERE p.created >= ?1 AND p.created <= ?2 AND p.published = true \
                ORDER BY p.created DESC""", startDateTime, endDateTime).list();
        return this.toResponseDtos(posts);
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

        final List<Object[]> countResults = PostCommentEntity.getEntityManager().createQuery(
                "SELECT c.post.id, COUNT(c) FROM PostCommentEntity c WHERE c.post.id IN :ids GROUP BY c.post.id",
                Object[].class).setParameter("ids", ids).getResultList();
        final Map<Long, Long> commentCounts = new HashMap<>();
        for (final Object[] row : countResults) {
            commentCounts.put((Long) row[0], (Long) row[1]);
        }

        return posts.stream().map(p -> new PostResponseDto(p, commentCounts.getOrDefault(p.id, 0L))).toList();
    }
}
