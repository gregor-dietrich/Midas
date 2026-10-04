package de.vptr.midas.gui.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

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
        final Integer safePage = PaginationParamUtil.sanitizePage(page);
        final Integer safeSize = PaginationParamUtil.sanitizeSize(size);
        return ServiceHelper.execute(() -> this.commentClient.getAllComments(safePage, safeSize), this.authService, LOG,
                "fetching comments");
    }

    /** Returns the comments by post. */
    public List<PostCommentResponseDto> getCommentsByPost(final String postId) {
        LOG.infof("Getting comments for post: %s", postId);
        return ServiceHelper.execute(() -> {
            final var response = this.commentClient.getCommentsByPost(postId, null, null);
            LOG.infof("Successfully retrieved %s comments for post %s", response.size(), postId);
            return response;
        }, this.authService, LOG, "getting comments for post " + postId);
    }

    /** Returns the comments by user. */
    public List<PostCommentResponseDto> getCommentsByUser(final String userId) {
        LOG.debugf("Fetching comments for user: %s", userId);
        return ServiceHelper.execute(() -> this.commentClient.getCommentsByUser(userId, null, null), this.authService,
                LOG, "fetching comments for user " + userId);
    }

    /** Handles the search comments operation. */
    public List<PostCommentResponseDto> searchComments(final String query) {
        LOG.debugf("Searching comments with query: %s", query);
        return ServiceHelper.execute(() -> this.commentClient.searchComments(query), this.authService, LOG,
                "searching comments");
    }

    /** Returns the comment by id. */
    public PostCommentDto getCommentById(final String id) {
        LOG.infof("Getting comment by id: %s", id);
        final var comment =
                PostCommentDto
                        .from(ServiceHelper
                                .executeOptional(() -> this.commentClient.getComment(id), this.authService, LOG,
                                        "getting comment " + id)
                                .orElseThrow(() -> new ServiceException("Comment not found")));
        LOG.infof("Successfully retrieved comment: %s", id);
        return comment;
    }

    /** Creates a new comment. */
    public PostCommentDto createComment(final PostCommentDto comment) {
        LOG.infof("Creating comment for post: %s",
                comment.getPostPublicId() != null ? comment.getPostPublicId() : "unknown");
        final var apiComment = new PostCommentDto();
        apiComment.setContent(comment.getContent());
        apiComment.setPostPublicId(comment.getPostPublicId());
        final var createdComment =
                PostCommentDto.from(ServiceHelper.execute(() -> this.commentClient.createComment(apiComment),
                        this.authService, LOG, "creating comment", "Failed to create comment"));
        LOG.infof("Successfully created comment: %s", createdComment.getPublicId());
        this.commentEvent.fire("comment-created:" + createdComment.getPostPublicId());
        return createdComment;
    }

    /** Updates the comment. */
    public PostCommentDto updateComment(final PostCommentDto comment) {
        final String publicId = Objects.requireNonNull(comment.getPublicId(), "comment.publicId");
        LOG.infof("Updating comment: %s", publicId);
        final var apiComment = new PostCommentDto();
        apiComment.setContent(comment.getContent());
        final var updatedComment =
                PostCommentDto.from(ServiceHelper.execute(() -> this.commentClient.updateComment(publicId, apiComment),
                        this.authService, LOG, "updating comment " + publicId, "Failed to update comment"));
        LOG.infof("Successfully updated comment: %s", updatedComment.getPublicId());
        return updatedComment;
    }

    /** Handles the delete comment operation. */
    public void deleteComment(final String id) {
        LOG.infof("Deleting comment: %s", id);
        ServiceHelper.executeVoid(() -> this.commentClient.deleteComment(id), this.authService, LOG,
                "deleting comment " + id, "Comment not found");
        LOG.infof("Successfully deleted comment: %s", id);
    }

    /** Returns the comments by date range. */
    public List<PostCommentResponseDto> getCommentsByDateRange(final LocalDate startDate, final LocalDate endDate) {
        LOG.debugf("Fetching comments by date range: %s to %s", startDate, endDate);
        return ServiceHelper.execute(() -> this.commentClient.getCommentsByDateRange(endDate, startDate),
                this.authService, LOG, "fetching comments by date range");
    }
}
