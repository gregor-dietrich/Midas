package de.vptr.midas.gui.service;

import java.time.LocalDate;
import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import de.vptr.midas.gui.client.PostCommentClient;
import de.vptr.midas.gui.dto.PostCommentDto;
import de.vptr.midas.gui.dto.PostCommentResponseDto;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.PaginationParamUtil;
import de.vptr.midas.gui.util.ServiceHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

/** Service for post comment operations. */
@ApplicationScoped
public class PostCommentService {

    private static final Logger LOG = Logger.getLogger(PostCommentService.class);

    @Inject
    @RestClient
    PostCommentClient commentClient;

    @Inject
    AuthService authService;

    @Inject
    Event<String> commentEvent;

    /** Returns the all comments. */
    public List<PostCommentResponseDto> getAllComments(final Integer page, final Integer size) {
        LOG.debug("Fetching all comments");
        final var authHeader = this.authService.requireAuthorizationHeader();
        final Integer safePage = PaginationParamUtil.sanitizePage(page);
        final Integer safeSize = PaginationParamUtil.sanitizeSize(size);
        return ServiceHelper.execute(() -> this.commentClient.getAllComments(safePage, safeSize, authHeader),
                this.authService, LOG, "fetching comments");
    }

    /** Returns the comments by post. */
    public List<PostCommentResponseDto> getCommentsByPost(final Long postId) {
        LOG.infof("Getting comments for post: %s", postId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> {
            final var response = this.commentClient.getCommentsByPost(postId, authHeader);
            LOG.infof("Successfully retrieved %s comments for post %s", response.size(), postId);
            return response;
        }, this.authService, LOG, "getting comments for post " + postId);
    }

    /** Returns the comments by user. */
    public List<PostCommentResponseDto> getCommentsByUser(final Long userId) {
        LOG.debugf("Fetching comments for user: %s", userId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.commentClient.getCommentsByUser(userId, authHeader), this.authService,
                LOG, "fetching comments for user " + userId);
    }

    /** Handles the search comments operation. */
    public List<PostCommentResponseDto> searchComments(final String query) {
        LOG.debugf("Searching comments with query: %s", query);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.commentClient.searchComments(query, authHeader), this.authService, LOG,
                "searching comments");
    }

    /** Returns the comment by id. */
    public PostCommentDto getCommentById(final Long id) {
        LOG.infof("Getting comment by id: %s", id);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.commentClient.getComment(id, authHeader), this.authService,
                LOG, "getting comment " + id, response -> {
                    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                        final var responseDto =
                                ServiceHelper.readEntity(response, PostCommentResponseDto.class, "comment response");
                        final var comment = responseDto.toPostCommentDto();
                        LOG.infof("Successfully retrieved comment: %s", id);
                        return comment;
                    } else {
                        throw new ServiceException("Comment not found");
                    }
                });
    }

    /** Creates a new comment. */
    public PostCommentDto createComment(final PostCommentDto comment) {
        LOG.infof("Creating comment for post: %s", comment.getPostId() != null ? comment.getPostId() : "unknown");
        final var authHeader = this.authService.requireAuthorizationHeader();
        final var apiComment = new PostCommentDto();
        apiComment.setContent(comment.getContent());
        apiComment.setPostId(comment.getPostId());
        return ServiceHelper.executeWithResponse(() -> this.commentClient.createComment(apiComment, authHeader),
                this.authService, LOG, "creating comment", "Failed to create comment", response -> {
                    if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
                        final var responseDto = ServiceHelper.readEntity(response, PostCommentResponseDto.class,
                                "created comment response");
                        final var createdComment = responseDto.toPostCommentDto();
                        LOG.infof("Successfully created comment: %s", createdComment.getId());
                        this.commentEvent.fire("comment-created:" + createdComment.getPostId());
                        return createdComment;
                    }
                    throw ServiceHelper.responseFailure("Failed to create comment", response);
                });
    }

    /** Updates the comment. */
    public PostCommentDto updateComment(final PostCommentDto comment) {
        LOG.infof("Updating comment: %s", comment.getId());
        final var authHeader = this.authService.requireAuthorizationHeader();
        final var apiComment = new PostCommentDto();
        apiComment.setContent(comment.getContent());
        return ServiceHelper.executeWithResponse(
                () -> this.commentClient.updateComment(comment.getId(), apiComment, authHeader), this.authService, LOG,
                "updating comment " + comment.getId(), "Failed to update comment", response -> {
                    if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                        final var responseDto = ServiceHelper.readEntity(response, PostCommentResponseDto.class,
                                "updated comment response");
                        final var updatedComment = responseDto.toPostCommentDto();
                        LOG.infof("Successfully updated comment: %s", updatedComment.getId());
                        return updatedComment;
                    }
                    throw ServiceHelper.responseFailure("Failed to update comment", response);
                });
    }

    /** Handles the delete comment operation. */
    public void deleteComment(final Long id) {
        LOG.infof("Deleting comment: %s", id);
        final var authHeader = this.authService.requireAuthorizationHeader();
        ServiceHelper.executeWithResponse(() -> this.commentClient.deleteComment(id, authHeader), this.authService, LOG,
                "deleting comment " + id, response -> {
                    if (response.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
                        LOG.infof("Successfully deleted comment: %s", id);
                        return null;
                    } else if (response.getStatus() == Response.Status.NOT_FOUND.getStatusCode()) {
                        throw new ServiceException("Comment not found");
                    }
                    throw ServiceHelper.responseFailure("Failed to delete comment", response);
                });
    }

    /** Returns the comments by date range. */
    public List<PostCommentResponseDto> getCommentsByDateRange(final LocalDate startDate, final LocalDate endDate) {
        LOG.debugf("Fetching comments by date range: %s to %s", startDate, endDate);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.commentClient.getCommentsByDateRange(startDate, endDate, authHeader),
                this.authService, LOG, "fetching comments by date range");
    }
}
