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
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.PostCommentService;
import de.vptr.midas.gui.util.NotificationUtil;

@SuppressWarnings("NullAway")
/**
 * Dialog for editing post comments in the admin view. Encapsulates form fields, binder, and save/cancel logic.
 */
public class AdminPostCommentDialog extends FormDialog {

    private static final Logger LOG = Logger.getLogger(AdminPostCommentDialog.class);

    private final PostCommentService commentService;
    private final Runnable onSave;
    private final Runnable onAuthFailure;

    private Binder<PostCommentDto> binder;
    private PostCommentDto currentComment;

    /** Initializes AdminPostCommentDialog. */
    public AdminPostCommentDialog(final PostCommentService commentService, final Runnable onSave,
            final Runnable onAuthFailure) {
        super();
        this.commentService = commentService;
        this.onSave = onSave;
        this.onAuthFailure = onAuthFailure;
    }

    /** Opens the dialog for the given comment. */
    public void open(final PostCommentDto comment) {
        this.removeAll();
        this.currentComment = comment != null ? comment : new PostCommentDto();
        this.binder = new Binder<>(PostCommentDto.class);

        final var title = new H3(comment != null && comment.getId() != null ? "Edit Comment" : "Create Comment");

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        final var contentField = new TextArea("Content");
        contentField.setRequired(true);
        contentField.setWidthFull();
        contentField.setHeight("200px");
        contentField.setMaxLength(5000);
        contentField.setInvalid(false);

        this.binder.bind(contentField, PostCommentDto::getContent, PostCommentDto::setContent);

        form.add(contentField);

        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var saveButton = new Button("Save", _ -> this.saveComment());
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
            this.binder.writeBean(this.currentComment);

            if (this.currentComment.getId() == null) {
                this.commentService.createComment(this.currentComment);
                NotificationUtil.showSuccess("Comment created successfully");
            } else {
                this.commentService.updateComment(this.currentComment);
                NotificationUtil.showSuccess("Comment updated successfully");
            }

            this.close();
            this.onSave.run();

        } catch (final ValidationException e) {
            NotificationUtil.showError("Please check the form for errors");
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.onAuthFailure.run();
        } catch (final ServiceException e) {
            NotificationUtil.showError(e, "Saving comment failed. Please try again.");
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error saving comment", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }
}
