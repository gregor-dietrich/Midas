package de.vptr.midas.gui.component.dialog;

import java.util.Objects;

import com.vaadin.flow.component.textfield.TextArea;

import de.vptr.midas.gui.dto.PostCommentDto;
import de.vptr.midas.gui.dto.PostResponseDto;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.PostCommentService;
import de.vptr.midas.gui.util.NotificationUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;

/** Dialog for adding comments to posts in the admin view. */
public class AdminPostCommentPanel extends AbstractPostCommentDialogBase {

    @Nullable
    private transient PostResponseDto selectedPost;

    /** Creates a new AdminPostCommentPanel. */
    public AdminPostCommentPanel(final PostCommentService postCommentService, final Runnable onAuthFailure) {
        super(postCommentService, null, onAuthFailure);
    }

    /** Opens the dialog for the selected post. */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Post reference intentionally stored for use in save operation")
    public void open(final PostResponseDto post) {
        this.selectedPost = post;
        this.currentComment = new PostCommentDto();
        this.initLayout("Add Comment to: " + post.getTitle(), "Post Comment", "Write your comment here...");
    }

    @Override
    protected String getContentLabel() {
        return "Comment";
    }

    @Override
    protected void configureBinding(final TextArea contentField) {
        Objects.requireNonNull(this.binder).forField(contentField)
                .withValidator(value -> value != null && !value.trim().isEmpty(), "Comment is required")
                .bind(PostCommentDto::getContent, PostCommentDto::setContent);
    }

    @Override
    protected void performSave() {
        final PostCommentDto comment = Objects.requireNonNull(this.currentComment);
        final PostResponseDto post = Objects.requireNonNull(this.selectedPost);
        comment.setPostPublicId(post.getPublicId());
        comment.setPostTitle(post.getTitle());

        this.commentService.createComment(comment);
        NotificationUtil.showSuccess("Comment added successfully");
    }

    /** Shows the service's message, which carries the backend's reason when it refuses the comment (422). */
    @Override
    protected void handleServiceException(final ServiceException e, final String entityName) {
        final String errorMessage = e.getMessage();
        if (errorMessage != null) {
            NotificationUtil.showError(e, errorMessage);
        } else {
            super.handleServiceException(e, entityName);
        }
    }
}
