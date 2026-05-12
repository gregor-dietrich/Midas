package de.vptr.midas.gui.component.dialog;

import com.vaadin.flow.component.textfield.TextArea;

import de.vptr.midas.gui.dto.PostCommentDto;
import de.vptr.midas.gui.dto.PostResponseDto;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.PostCommentService;
import de.vptr.midas.gui.util.NotificationUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/** Dialog for adding comments to posts in the admin view. */
@SuppressWarnings("NullAway")
@SuppressFBWarnings(value = "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR",
        justification = "Fields initialized lazily in open(), not constructor")
public class AdminPostCommentPanel extends AbstractPostCommentDialogBase {

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
        this.binder.forField(contentField)
                .withValidator(value -> value != null && !value.trim().isEmpty(), "Comment is required")
                .bind(PostCommentDto::getContent, PostCommentDto::setContent);
    }

    @Override
    protected void performSave() {
        this.currentComment.setPostId(this.selectedPost.getId());
        this.currentComment.setPostTitle(this.selectedPost.getTitle());

        this.commentService.createComment(this.currentComment);
        NotificationUtil.showSuccess("Comment added successfully");
    }

    @Override
    protected void handleServiceException(final ServiceException e, final String entityName) {
        final String errorMessage = e.getMessage();
        if (errorMessage != null && errorMessage.contains("Bad Request")) {
            NotificationUtil.showError("Post is not commentable");
        } else {
            super.handleServiceException(e, entityName);
        }
    }
}
