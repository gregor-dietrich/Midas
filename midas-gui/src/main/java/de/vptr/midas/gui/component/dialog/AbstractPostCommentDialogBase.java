package de.vptr.midas.gui.component.dialog;

import java.util.Objects;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.Binder;

import de.vptr.midas.gui.dto.PostCommentDto;
import de.vptr.midas.gui.service.PostCommentService;
import jakarta.annotation.Nullable;

/** Base dialog class for editing or adding post comments. */
@SuppressWarnings("NullAway")
public abstract class AbstractPostCommentDialogBase extends AbstractAdminEntityDialogBase<PostCommentDto> {

    protected final transient PostCommentService commentService;

    @Nullable
    protected transient PostCommentDto currentComment;

    protected AbstractPostCommentDialogBase(final PostCommentService commentService, @Nullable final Runnable onSave,
            final Runnable onAuthFailure) {
        super(onSave, onAuthFailure);
        this.commentService = commentService;
    }

    protected void initLayout(final String titleText, final String saveButtonText, @Nullable final String placeholder) {
        this.removeAll();
        this.currentBean = this.currentComment != null ? this.currentComment : new PostCommentDto();
        this.binder = new Binder<>(PostCommentDto.class);

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        final var contentField = new TextArea(this.getContentLabel());
        contentField.setRequired(true);
        contentField.setWidthFull();
        contentField.setHeight("200px");
        contentField.setMaxLength(5000);
        if (placeholder != null) {
            contentField.setPlaceholder(placeholder);
        }
        contentField.setInvalid(false);

        this.configureBinding(contentField);

        form.add(contentField);

        this.initDialogLayout(titleText, form);
    }

    protected String getContentLabel() {
        return "Content";
    }

    protected void configureBinding(final TextArea contentField) {
        Objects.requireNonNull(this.binder).bind(contentField, PostCommentDto::getContent, PostCommentDto::setContent);
    }

    @Override
    protected void handleSave() {
        this.currentComment = this.currentBean;
        this.executeSaveWorkflow(true, "Comment saved successfully", "Comment saved successfully",
                dto -> this.performSave(), dto -> this.performSave(), "Comment");
    }

    protected abstract void performSave();
}
