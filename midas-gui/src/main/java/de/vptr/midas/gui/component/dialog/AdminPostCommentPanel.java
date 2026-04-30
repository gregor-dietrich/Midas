package de.vptr.midas.gui.component.dialog;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;

import de.vptr.midas.gui.dto.PostCommentDto;
import de.vptr.midas.gui.dto.PostResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.PostCommentService;
import de.vptr.midas.gui.util.NotificationUtil;

/**
 * Dialog for adding comments to posts in the admin view.
 * Encapsulates the comment form, binder, and save logic.
 */
public class AdminPostCommentPanel extends FormDialog {

    private static final Logger LOG = Logger.getLogger(AdminPostCommentPanel.class);

    private final PostCommentService postCommentService;
    private final Runnable onAuthFailure;

    private Binder<PostCommentDto> binder;
    private PostCommentDto currentComment;
    private PostResponseDto selectedPost;

    /** Initializes AdminPostCommentPanel. */
    public AdminPostCommentPanel(final PostCommentService postCommentService,
            final Runnable onAuthFailure) {
        super();
        this.postCommentService = postCommentService;
        this.onAuthFailure = onAuthFailure;
    }

    /** Opens the dialog. */
    public void open(final PostResponseDto post) {
        this.selectedPost = post;
        this.removeAll();
        this.currentComment = new PostCommentDto();
        this.binder = new Binder<>(PostCommentDto.class);

        final var title = new H3("Add Comment to: " + post.getTitle());

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        final var contentField = new TextArea("Comment");
        contentField.setRequired(true);
        contentField.setWidthFull();
        contentField.setHeight("200px");
        contentField.setMaxLength(5000);
        contentField.setPlaceholder("Write your comment here...");
        contentField.setInvalid(false);

        this.binder.forField(contentField)
                .withValidator(value -> value != null && !value.trim().isEmpty(), "Comment is required")
                .bind(PostCommentDto::getContent, PostCommentDto::setContent);

        form.add(contentField);

        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var saveButton = new Button("Post Comment", _ -> this.saveComment());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var cancelButton = new Button("Cancel", _ -> this.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttonLayout.add(saveButton, cancelButton);

        final var dialogLayout = new VerticalLayout(title, form, buttonLayout);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);
        dialogLayout.setSizeFull();

        this.add(dialogLayout);
        this.binder.readBean(this.currentComment);
        super.open();
    }

    private void saveComment() {
        try {
            if (!this.binder.validate().isOk()) {
                NotificationUtil.showError("Please check the form for errors");
                return;
            }

            this.binder.writeBean(this.currentComment);

            this.currentComment.setPostId(this.selectedPost.getId());
            this.currentComment.setPostTitle(this.selectedPost.getTitle());

            this.postCommentService.createComment(this.currentComment);
            NotificationUtil.showSuccess("Comment added successfully");

            this.close();

        } catch (final ValidationException e) {
            NotificationUtil.showError("Please check the form for errors");
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.onAuthFailure.run();
        } catch (final ServiceException e) {
            final String errorMessage = e.getMessage();
            if (errorMessage != null && errorMessage.contains("Bad Request")) {
                NotificationUtil.showError("Post is not commentable");
            } else {
                NotificationUtil.showError(e, "Saving comment failed. Please try again.");
            }
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error saving comment", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }
}
