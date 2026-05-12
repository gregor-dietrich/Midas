package de.vptr.midas.gui.component.dialog;

import de.vptr.midas.gui.dto.PostCommentDto;
import de.vptr.midas.gui.service.PostCommentService;
import de.vptr.midas.gui.util.NotificationUtil;

/** Dialog for editing post comments in the admin view. */
@SuppressWarnings("NullAway")
public class AdminPostCommentDialog extends AbstractPostCommentDialogBase {

    /** Creates a new AdminPostCommentDialog. */
    public AdminPostCommentDialog(final PostCommentService commentService, final Runnable onSave,
            final Runnable onAuthFailure) {
        super(commentService, onSave, onAuthFailure);
    }

    /** Opens the dialog for editing or creating a comment. */
    public void open(final PostCommentDto comment) {
        this.currentComment = comment != null ? comment : new PostCommentDto();
        final var titleText = comment != null && comment.getId() != null ? "Edit Comment" : "Create Comment";
        this.initLayout(titleText, "Save", null);
    }

    @Override
    protected void performSave() {
        if (this.currentComment.getId() == null) {
            this.commentService.createComment(this.currentComment);
            NotificationUtil.showSuccess("Comment created successfully");
        } else {
            this.commentService.updateComment(this.currentComment);
            NotificationUtil.showSuccess("Comment updated successfully");
        }
        if (this.onSave != null) {
            this.onSave.run();
        }
    }
}
