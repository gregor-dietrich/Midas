package de.vptr.midas.api.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import de.vptr.midas.api.dto.PostCommentDto;
import de.vptr.midas.api.dto.PostCommentResponseDto;
import de.vptr.midas.api.entity.PostCommentEntity;
import de.vptr.midas.api.entity.PostEntity;
import de.vptr.midas.api.entity.UserEntity;
import de.vptr.midas.api.util.SearchQueryUtil;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
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

    @Inject
    Event<String> commentEvent;

    /** Returns all comments with default pagination. */
    public List<PostCommentResponseDto> getAllComments() {
        return this.getAllComments(0, 20);
    }

    /** Returns all comments with the given pagination. */
    public List<PostCommentResponseDto> getAllComments(final int page, final int size) {
        final List<PostCommentEntity> comments = PostCommentEntity.find(
                "SELECT c FROM PostCommentEntity c LEFT JOIN FETCH c.post LEFT JOIN FETCH c.user ORDER BY c.created DESC")
                .page(page, size).list();
        return comments.stream().map(PostCommentResponseDto::new).toList();
    }

    /** Returns the comment with the given publicId. */
    public Optional<PostCommentResponseDto> findById(final String publicId) {
        return PostCommentEntity.find(
                "SELECT c FROM PostCommentEntity c LEFT JOIN FETCH c.post LEFT JOIN FETCH c.user WHERE c.publicId = ?1",
                publicId).<PostCommentEntity>firstResultOptional().map(PostCommentResponseDto::new);
    }

    /** Returns all comments for the given post with default pagination. */
    public List<PostCommentResponseDto> findByPostPublicId(final String postPublicId) {
        return this.findByPostPublicId(postPublicId, 0, 20);
    }

    /** Returns all comments for the given post with pagination. */
    public List<PostCommentResponseDto> findByPostPublicId(final String postPublicId, final int page, final int size) {
        final List<PostCommentEntity> comments = PostCommentEntity.find(
                "SELECT c FROM PostCommentEntity c LEFT JOIN FETCH c.post LEFT JOIN FETCH c.user WHERE c.post.publicId = ?1",
                postPublicId).page(page, size).list();
        return comments.stream().map(PostCommentResponseDto::new).toList();
    }

    /** Returns all comments by the given user with default pagination. */
    public List<PostCommentResponseDto> findByUserPublicId(final String userPublicId) {
        return this.findByUserPublicId(userPublicId, 0, 20);
    }

    /** Returns all comments by the given user with pagination. */
    public List<PostCommentResponseDto> findByUserPublicId(final String userPublicId, final int page, final int size) {
        final List<PostCommentEntity> comments = PostCommentEntity.find(
                "SELECT c FROM PostCommentEntity c LEFT JOIN FETCH c.post LEFT JOIN FETCH c.user WHERE c.user.publicId = ?1",
                userPublicId).page(page, size).list();
        return comments.stream().map(PostCommentResponseDto::new).toList();
    }

    /** Returns the most recent comments up to the given limit. */
    public List<PostCommentResponseDto> findRecentComments(final int limit) {
        final List<PostCommentEntity> comments = PostCommentEntity.find(
                "SELECT c FROM PostCommentEntity c LEFT JOIN FETCH c.post LEFT JOIN FETCH c.user ORDER BY c.created DESC")
                .page(0, limit).list();
        return comments.stream().map(PostCommentResponseDto::new).toList();
    }

    /** Creates a new comment, assigning the current user. */
    @Transactional
    public PostCommentResponseDto createComment(final PostCommentDto dto, final String currentUsername) {
        final PostEntity existingPost = PostEntity.find("publicId", dto.postPublicId).firstResult();
        if (existingPost == null) {
            throw new WebApplicationException("Post with Public ID " + dto.postPublicId + " does not exist.",
                    Response.Status.BAD_REQUEST);
        }
        if (existingPost.published == null || !existingPost.published) {
            throw new WebApplicationException("Cannot add comment to an unpublished post.",
                    Response.Status.BAD_REQUEST);
        }
        if (existingPost.commentable == null || !existingPost.commentable) {
            throw new WebApplicationException("Comments are not allowed on this post.", Response.Status.BAD_REQUEST);
        }
        final PostCommentEntity comment = new PostCommentEntity();
        comment.content = dto.content;
        comment.post = existingPost;
        comment.user = (UserEntity) UserEntity.find("username", currentUsername).firstResultOptional()
                .orElseThrow(() -> new WebApplicationException("User with username '" + currentUsername + "' not found",
                        Response.Status.NOT_FOUND));
        comment.persistAndFlush();
        this.commentEvent.fire("comment-created:" + comment.post.publicId);
        return new PostCommentResponseDto(comment);
    }

    /** Updates the comment with the given publicId. */
    @Transactional
    public PostCommentResponseDto updateComment(final String publicId, final PostCommentDto dto,
            final String currentUsername) {
        final PostCommentEntity existingComment = PostCommentEntity.find("publicId", publicId).firstResult();
        if (existingComment == null) {
            throw new WebApplicationException("Comment not found", Response.Status.NOT_FOUND);
        }
        this.verifyCommentOwnership(currentUsername, existingComment, "Access denied to edit comment");
        existingComment.content = dto.content;
        existingComment.persistAndFlush();
        return new PostCommentResponseDto(existingComment);
    }

    /** Partially updates the comment with the given publicId. */
    @Transactional
    public PostCommentResponseDto patchComment(final String publicId, final PostCommentDto dto,
            final String currentUsername) {
        final PostCommentEntity existingComment = PostCommentEntity.find("publicId", publicId).firstResult();
        if (existingComment == null) {
            throw new WebApplicationException("Comment not found", Response.Status.NOT_FOUND);
        }
        this.verifyCommentOwnership(currentUsername, existingComment, "Access denied to patch comment");
        if (dto.content != null) {
            existingComment.content = dto.content;
        }
        existingComment.persistAndFlush();
        return new PostCommentResponseDto(existingComment);
    }

    /** Deletes the comment with the given publicId. */
    @Transactional
    public boolean deleteComment(final String publicId, @Nullable final String currentUsername) {
        final PostCommentEntity existingComment = PostCommentEntity.find("publicId", publicId).firstResult();
        if (existingComment == null) {
            return false;
        }

        this.verifyCommentOwnership(currentUsername, existingComment, "Access denied to delete comment");

        return PostCommentEntity.delete("publicId", publicId) > 0;
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

        return comments.stream().map(PostCommentResponseDto::new).toList();
    }

    /** Returns comments within the given date range. */
    public List<PostCommentResponseDto> findByDateRange(final LocalDate startDate, final LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return List.of();
        }

        final LocalDateTime startDateTime = startDate.atStartOfDay();
        final LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        final List<PostCommentEntity> comments = PostCommentEntity.find(
                "SELECT c FROM PostCommentEntity c LEFT JOIN FETCH c.post LEFT JOIN FETCH c.user WHERE c.created >= ?1 AND c.created <= ?2",
                startDateTime, endDateTime).list();

        return comments.stream().map(PostCommentResponseDto::new).toList();
    }

    private void verifyCommentOwnership(@Nullable final String currentUsername, final PostCommentEntity existingComment,
            final String deniedMessage) {
        if (currentUsername == null) {
            throw new ValidationException("Authentication required to modify comments.");
        }
        if (existingComment.user == null) {
            throw new ValidationException("Comment has no owner and cannot be modified.");
        }
        if (!currentUsername.equals(existingComment.user.username)
                && !this.adminAccessService.hasAdminViewAccess(currentUsername)) {
            throw new WebApplicationException(deniedMessage, Response.Status.FORBIDDEN);
        }
    }
}
