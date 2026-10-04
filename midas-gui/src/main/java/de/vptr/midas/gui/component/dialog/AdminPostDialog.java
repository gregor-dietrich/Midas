package de.vptr.midas.gui.component.dialog;

import java.util.List;
import java.util.Objects;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;

import de.vptr.midas.gui.dto.PostCategoryResponseDto;
import de.vptr.midas.gui.dto.PostDto;
import de.vptr.midas.gui.dto.PostResponseDto;
import de.vptr.midas.gui.service.PostService;
import de.vptr.midas.gui.service.UserService;
import de.vptr.midas.gui.util.NotificationUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;

/**
 * Dialog for creating and editing posts in the admin view. Encapsulates form fields, binder, and save/cancel logic.
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Service reference intentionally stored for runtime behavior")
public class AdminPostDialog extends AbstractAdminEntityDialogBase<PostDto> {

    private static final Logger LOG = Logger.getLogger(AdminPostDialog.class);

    private final transient PostService postService;
    private final transient UserService userService;

    /** Initializes AdminPostDialog. */
    public AdminPostDialog(final PostService postService, final UserService userService, final Runnable onSave,
            final Runnable onAuthFailure) {
        super(onSave, onAuthFailure);
        this.postService = postService;
        this.userService = userService;
    }

    /** Opens the dialog. */
    public void open(@Nullable final PostResponseDto post,
            @Nullable final List<PostCategoryResponseDto> availableCategories) {
        this.removeAll();
        this.currentBean = post != null ? PostDto.from(post) : new PostDto();
        this.binder = new Binder<>(PostDto.class);

        if (post == null) {
            try {
                final var currentUser = this.userService.getCurrentUser();
                if (currentUser.isPresent()) {
                    this.currentBean.setUsername(currentUser.get().getUsername());
                } else {
                    LOG.warn("Current user not found when creating new post");
                    NotificationUtil.showError("Unable to determine current user. Please try logging in again.");
                    return;
                }
            } catch (final RuntimeException e) {
                LOG.error("Error retrieving current user for new post", e);
                NotificationUtil.showError("Error retrieving user information. Please try again.");
                return;
            }
        }

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        final var titleField = new TextField("Title");
        titleField.setRequired(true);
        titleField.setWidthFull();
        titleField.setMaxLength(255);
        titleField.setInvalid(false);

        final var contentField = new TextArea("Content");
        contentField.setRequired(true);
        contentField.setWidthFull();
        contentField.setHeight("200px");
        contentField.setMaxLength(50_000);
        contentField.setInvalid(false);

        final var publishedField = new Checkbox("Published");
        final var commentableField = new Checkbox("Commentable");

        final var categoryField = new ComboBox<PostCategoryResponseDto>("Category");
        categoryField.setItems(availableCategories != null ? availableCategories : List.of());
        categoryField.setItemLabelGenerator(PostCategoryResponseDto::getName);
        categoryField.setPlaceholder("(none)");
        categoryField.setClearButtonVisible(true);
        categoryField.setInvalid(false);

        this.binder.forField(titleField)
                .withValidator(value -> value != null && !value.trim().isEmpty(), "Title is required")
                .bind(PostDto::getTitle, PostDto::setTitle);
        this.binder.forField(contentField)
                .withValidator(value -> value != null && !value.trim().isEmpty(), "Content is required")
                .bind(PostDto::getContent, PostDto::setContent);
        this.binder.bind(publishedField, p -> {
            final var published = p.isPublished();
            return published != null ? published : false;
        }, PostDto::setPublished);
        this.binder.bind(commentableField, p -> {
            final var commentable = p.isCommentable();
            return commentable != null ? commentable : false;
        }, PostDto::setCommentable);

        this.binder.bind(categoryField, p -> {
            if (p.getCategoryPublicId() != null && availableCategories != null) {
                return availableCategories.stream().filter(cat -> cat.getPublicId().equals(p.getCategoryPublicId()))
                        .findFirst().orElse(null);
            }
            return null;
        }, (p, value) -> {
            if (value != null) {
                p.setCategoryPublicId(value.getPublicId());
                p.setCategoryName(value.getName());
            } else {
                p.setCategoryPublicId(null);
                p.setCategoryName(null);
            }
        });

        form.add(titleField, contentField, categoryField, publishedField, commentableField);

        this.initDialogLayout(post != null ? "Edit Post" : "Create Post", form);
    }

    @Override
    protected boolean preSave(final PostDto bean) {
        bean.setCreated(null);
        bean.setLastEdit(null);
        return true;
    }

    @Override
    protected void handleSave() {
        final boolean isNew = Objects.requireNonNull(this.currentBean).getPublicId() == null;
        this.executeSaveWorkflow(isNew, "Post created successfully", "Post updated successfully",
                this.postService::createPost, this.postService::updatePost, "Post");
    }
}
