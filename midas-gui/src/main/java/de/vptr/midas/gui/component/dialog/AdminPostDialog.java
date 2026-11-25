package de.vptr.midas.gui.component.dialog;

import java.util.List;
import java.util.Objects;

import jakarta.annotation.Nullable;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
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
        final var bean = post != null ? PostDto.from(post) : new PostDto();
        this.currentBean = bean;
        final var b = new Binder<>(PostDto.class);
        this.binder = b;

        if (post == null && !this.assignCurrentAuthorForNewPost(bean)) {
            return;
        }

        final var form = this.createResponsiveFormLayout();

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

        bindPostFields(b, titleField, contentField, publishedField, commentableField, categoryField,
                availableCategories);

        form.add(titleField, contentField, categoryField, publishedField, commentableField);

        this.initDialogLayout(post != null ? "Edit Post" : "Create Post", form);
    }

    private boolean assignCurrentAuthorForNewPost(final PostDto bean) {
        try {
            final var currentUser = this.userService.getCurrentUser();
            if (currentUser.isPresent()) {
                bean.setUsername(currentUser.get().getUsername());
                return true;
            }
            LOG.warn("Current user not found when creating new post");
            NotificationUtil.showError("Unable to determine current user. Please try logging in again.");
            return false;
        } catch (final RuntimeException e) {
            LOG.error("Error retrieving current user for new post", e);
            NotificationUtil.showError("Error retrieving user information. Please try again.");
            return false;
        }
    }

    private static void bindPostFields(final Binder<PostDto> b, final TextField titleField, final TextArea contentField,
            final Checkbox publishedField, final Checkbox commentableField,
            final ComboBox<PostCategoryResponseDto> categoryField,
            @Nullable final List<PostCategoryResponseDto> availableCategories) {
        b.forField(titleField).withValidator(value -> value != null && !value.trim().isEmpty(), "Title is required")
                .bind(PostDto::getTitle, PostDto::setTitle);
        b.forField(contentField).withValidator(value -> value != null && !value.trim().isEmpty(), "Content is required")
                .bind(PostDto::getContent, PostDto::setContent);
        b.bind(publishedField, p -> Boolean.TRUE.equals(p.isPublished()), PostDto::setPublished);
        b.bind(commentableField, p -> Boolean.TRUE.equals(p.isCommentable()), PostDto::setCommentable);
        b.bind(categoryField, p -> findCategoryByPublicId(availableCategories, p.getCategoryPublicId()),
                AdminPostDialog::applyCategorySelection);
    }

    @Nullable
    private static PostCategoryResponseDto findCategoryByPublicId(
            @Nullable final List<PostCategoryResponseDto> availableCategories,
            @Nullable final String categoryPublicId) {
        if (categoryPublicId == null || availableCategories == null) {
            return null;
        }
        return availableCategories.stream().filter(cat -> cat.getPublicId().equals(categoryPublicId)).findFirst()
                .orElse(null);
    }

    private static void applyCategorySelection(final PostDto post, @Nullable final PostCategoryResponseDto value) {
        if (value != null) {
            post.setCategoryPublicId(value.getPublicId());
            post.setCategoryName(value.getName());
        } else {
            post.setCategoryPublicId(null);
            post.setCategoryName(null);
        }
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
