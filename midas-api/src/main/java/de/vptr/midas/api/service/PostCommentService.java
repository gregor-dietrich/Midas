package de.vptr.midas.api.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import de.vptr.midas.api.dto.PostCommentResponseDto;
import de.vptr.midas.api.entity.PostCommentEntity;
import de.vptr.midas.api.entity.PostEntity;
import de.vptr.midas.api.entity.UserEntity;
import de.vptr.midas.api.util.SearchQueryUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/** Service for post comment business logic. */
@ApplicationScoped
public class PostCommentService {

    @Inject
    UserService userService;

    @Inject
    AdminAccessService adminAccessService;

    /** Returns all comments with default pagination. */
    public List<PostCommentResponseDto> getAllComments() {
        return this.getAllComments(0, 20);
    }

    /** Returns all comments with the given pagination. */
    public List<PostCommentResponseDto> getAllComments(final int page, final int size) {
        final List<PostCommentEntity> comments = PostCommentEntity
                .find("SELECT c FROM PostCommentEntity c LEFT JOIN FETCH c.post LEFT JOIN FETCH c.user ORDER BY c.created DESC")
                .page(page, size)
                .list();
        return comments.stream()
                .map(PostCommentResponseDto::new)
                .toList();
    }

    /** Returns the comment with the given id. */
    public Optional<PostCommentResponseDto> findById(final Long id) {
        return PostCommentEntity
                .find("SELECT c FROM PostCommentEntity c LEFT JOIN FETCH c.post LEFT JOIN FETCH c.user WHERE c.id = ?1",
                        id)
                .<PostCommentEntity>firstResultOptional()
                .map(PostCommentResponseDto::new);
    }

    /** Returns all comments for the given post. */
    public List<PostCommentResponseDto> findByPostId(final Long postId) {
        final List<PostCommentEntity> comments = PostCommentEntity
                .find("SELECT c FROM PostCommentEntity c LEFT JOIN FETCH c.post LEFT JOIN FETCH c.user WHERE c.post.id = ?1",
                        postId)
                .list();
        return comments.stream()
                .map(PostCommentResponseDto::new)
                .toList();
    }

    /** Returns all comments by the given user. */
    public List<PostCommentResponseDto> findByUserId(final Long userId) {
        final List<PostCommentEntity> comments = PostCommentEntity
                .find("SELECT c FROM PostCommentEntity c LEFT JOIN FETCH c.post LEFT JOIN FETCH c.user WHERE c.user.id = ?1",
                        userId)
                .list();
        return comments.stream()
                .map(PostCommentResponseDto::new)
                .toList();
    }

    /** Returns the most recent comments up to the given limit. */
    public List<PostCommentResponseDto> findRecentComments(final int limit) {
        final List<PostCommentEntity> comments = PostCommentEntity
                .find("SELECT c FROM PostCommentEntity c LEFT JOIN FETCH c.post LEFT JOIN FETCH c.user ORDER BY c.created DESC")
                .page(0, limit).list();
        return comments.stream()
                .map(PostCommentResponseDto::new)
                .toList();
    }

    /** Creates a new comment, assigning the current user. */
    @Transactional
    public PostCommentResponseDto createComment(final PostCommentEntity comment, final String currentUsername) {
        // Validate content is provided for creation
        if (comment.content == null || comment.content.trim().isEmpty()) {
            throw new ValidationException("Content is required for creating a comment");
        }

        // Validate post exists
        final PostEntity existingPost = PostEntity.findById(comment.post != null ? comment.post.id : null);
        if (existingPost == null) {
            throw new WebApplicationException(
                    "Post with ID " + (comment.post != null ? comment.post.id : null) + " does not exist.",
                    Response.Status.BAD_REQUEST);
        }

        // Validate post is published
        if (existingPost.published == null || !existingPost.published) {
            throw new WebApplicationException("Cannot add comment to an unpublished post.",
                    Response.Status.BAD_REQUEST);
        }

        // Validate post allows comments
        if (existingPost.commentable == null || !existingPost.commentable) {
            throw new WebApplicationException("Comments are not allowed on this post.", Response.Status.BAD_REQUEST);
        }

        // Always assign the managed post entity
        comment.post = existingPost;

        // Auto-assign current user if not provided
        if (comment.user == null) {
            comment.user = (UserEntity) UserEntity.find("username", currentUsername).firstResultOptional()
                    .orElseThrow(() -> new WebApplicationException(
                            "User with username '" + currentUsername + "' not found", Response.Status.NOT_FOUND));
        }

        comment.persistAndFlush();
        PostCommentEntity.getEntityManager().detach(comment);

        return new PostCommentResponseDto(PostCommentEntity
                .find("SELECT c FROM PostCommentEntity c LEFT JOIN FETCH c.post LEFT JOIN FETCH c.user WHERE c.id = ?1",
                        comment.id)
                .<PostCommentEntity>firstResult());
    }

    /** Updates the comment with the given id. */
    @Transactional
    public PostCommentResponseDto updateComment(final PostCommentEntity comment, final String currentUsername) {
        final PostCommentEntity existingComment = PostCommentEntity.findById(comment.id);
        if (existingComment == null) {
            throw new WebApplicationException("Comment not found", Response.Status.NOT_FOUND);
        }

        this.verifyCommentOwnership(currentUsername, existingComment, "Access denied to edit comment");

        // Validate content is provided for complete replacement (PUT)
        if (comment.content == null || comment.content.trim().isEmpty()) {
            throw new ValidationException("Content is required for updating a comment");
        }

        // Only update content field for PUT (since we only allow content in DTO)
        existingComment.content = comment.content;

        existingComment.persistAndFlush();
        PostCommentEntity.getEntityManager().detach(existingComment);

        return new PostCommentResponseDto(PostCommentEntity
                .find("SELECT c FROM PostCommentEntity c LEFT JOIN FETCH c.post LEFT JOIN FETCH c.user WHERE c.id = ?1",
                        existingComment.id)
                .<PostCommentEntity>firstResult());
    }

    /** Partially updates the comment with the given id. */
    @Transactional
    public PostCommentResponseDto patchComment(final PostCommentEntity comment, final String currentUsername) {
        final PostCommentEntity existingComment = PostCommentEntity.findById(comment.id);
        if (existingComment == null) {
            throw new WebApplicationException("Comment not found", Response.Status.NOT_FOUND);
        }

        this.verifyCommentOwnership(currentUsername, existingComment, "Access denied to patch comment");

        // Partial update (PATCH semantics) - only update provided fields
        if (comment.content != null) {
            existingComment.content = comment.content;
        }

        existingComment.persistAndFlush();
        PostCommentEntity.getEntityManager().detach(existingComment);

        return new PostCommentResponseDto(PostCommentEntity
                .find("SELECT c FROM PostCommentEntity c LEFT JOIN FETCH c.post LEFT JOIN FETCH c.user WHERE c.id = ?1",
                        existingComment.id)
                .<PostCommentEntity>firstResult());
    }

    /** Deletes the comment with the given id. */
    @Transactional
    public boolean deleteComment(final Long id, final String currentUsername) {
        final PostCommentEntity existingComment = PostCommentEntity.findById(id);
        if (existingComment == null) {
            return false;
        }

        this.verifyCommentOwnership(currentUsername, existingComment, "Access denied to delete comment");

        return PostCommentEntity.deleteById(id);
    }

    /** Returns all comments matching the given query. */
    public List<PostCommentResponseDto> searchComments(final String query) {
        if (query == null || query.trim().isEmpty()) {
            return this.getAllComments();
        }
        final String searchTerm = SearchQueryUtil.toContainsLikePattern(query);
        if (searchTerm == null) {
            return this.getAllComments();
        }
        final List<PostCommentEntity> comments = PostCommentEntity.find(
                "SELECT c FROM PostCommentEntity c LEFT JOIN FETCH c.post LEFT JOIN FETCH c.user WHERE LOWER(c.content) LIKE ?1 ESCAPE '\\' OR LOWER(c.user.username) LIKE ?1 ESCAPE '\\'",
                searchTerm).page(0, 100).list();

        return comments.stream()
                .map(PostCommentResponseDto::new)
                .toList();
    }

    /** Returns comments within the given date range. */
    public List<PostCommentResponseDto> findByDateRange(final String startDate, final String endDate) {
        if (startDate == null || endDate == null) {
            return this.getAllComments();
        }

        try {
            final LocalDate start = LocalDate.parse(startDate);
            final LocalDate end = LocalDate.parse(endDate);

            final LocalDateTime startDateTime = start.atStartOfDay();
            final LocalDateTime endDateTime = end.atTime(LocalTime.MAX);

            final List<PostCommentEntity> comments = PostCommentEntity
                    .find("SELECT c FROM PostCommentEntity c LEFT JOIN FETCH c.post LEFT JOIN FETCH c.user WHERE c.created >= ?1 AND c.created <= ?2",
                            startDateTime, endDateTime)
                    .list();

            return comments.stream()
                    .map(PostCommentResponseDto::new)
                    .toList();
        } catch (final DateTimeParseException e) {
            throw new ValidationException("Invalid date format. Expected: yyyy-MM-dd");
        }
    }

    private void verifyCommentOwnership(final String currentUsername, final PostCommentEntity existingComment,
            final String deniedMessage) {
        if (currentUsername == null) {
            throw new ValidationException("Authentication required to modify comments.");
        }
        if (existingComment.user == null) {
            throw new ValidationException("Comment has no owner and cannot be modified.");
        }
        if (!existingComment.user.username.equals(currentUsername)
                && !this.adminAccessService.hasAdminViewAccess(currentUsername)) {
            throw new WebApplicationException(deniedMessage, Response.Status.FORBIDDEN);
        }
    }
}
