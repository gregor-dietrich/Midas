package de.vptr.midas.gui.component.dialog;

import java.util.List;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;

import de.vptr.midas.gui.dto.PostCategoryResponseDto;
import de.vptr.midas.gui.dto.PostDto;
import de.vptr.midas.gui.dto.PostResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.PostService;
import de.vptr.midas.gui.service.UserService;
import de.vptr.midas.gui.util.NotificationUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;

/**
 * Dialog for creating and editing posts in the admin view. Encapsulates form fields, binder, and save/cancel logic.
 */
@SuppressWarnings("NullAway")
@SuppressFBWarnings(value = { "EI_EXPOSE_REP2", "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR" },
        justification = "Fields initialized lazily in open(), not constructor")
public class AdminPostDialog extends FormDialog {

    private static final Logger LOG = Logger.getLogger(AdminPostDialog.class);

    private final transient PostService postService;
    private final transient UserService userService;
    private final Runnable onSave;
    private final Runnable onAuthFailure;

    private Binder<PostDto> binder;
    private transient PostDto currentPost;

    /** Initializes AdminPostDialog. */
    public AdminPostDialog(final PostService postService, final UserService userService, final Runnable onSave,
            final Runnable onAuthFailure) {
        super();
        this.postService = postService;
        this.userService = userService;
        this.onSave = onSave;
        this.onAuthFailure = onAuthFailure;
    }

    /** Opens the dialog. */
    public void open(@Nullable final PostResponseDto post,
            @Nullable final List<PostCategoryResponseDto> availableCategories) {
        this.removeAll();
        this.currentPost = post != null ? post.toPostDto() : new PostDto();
        this.binder = new Binder<>(PostDto.class);

        // For new posts, automatically set the current user as the author
        if (post == null) {
            try {
                final var currentUser = this.userService.getCurrentUser();
                if (currentUser.isPresent()) {
                    this.currentPost.setUserId(currentUser.get().getId());
                    this.currentPost.setUsername(currentUser.get().getUsername());
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

        // CPD-OFF
        final var title = new H3(post != null ? "Edit Post" : "Create Post");

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
        // CPD-ON

        final var publishedField = new Checkbox("Published");
        final var commentableField = new Checkbox("Commentable");

        final var categoryField = new ComboBox<PostCategoryResponseDto>("Category");
        categoryField.setItems(availableCategories != null ? availableCategories : List.of());
        categoryField.setItemLabelGenerator(PostCategoryResponseDto::getName);
        categoryField.setPlaceholder("(none)");
        categoryField.setClearButtonVisible(true);
        categoryField.setInvalid(false);

        // Bind fields
        this.binder.forField(titleField)
                .withValidator(value -> value != null && !value.trim().isEmpty(), "Title is required")
                .bind(p -> p.getTitle(), (p, value) -> p.setTitle(value));
        this.binder.forField(contentField)
                .withValidator(value -> value != null && !value.trim().isEmpty(), "Content is required")
                .bind(p -> p.getContent(), (p, value) -> p.setContent(value));
        this.binder.bind(publishedField, p -> p.isPublished() != null ? p.isPublished() : false,
                (p, value) -> p.setPublished(value));
        this.binder.bind(commentableField, p -> p.isCommentable() != null ? p.isCommentable() : false,
                (p, value) -> p.setCommentable(value));

        // Category binding - convert between PostCategoryResponseDto and categoryId
        this.binder.bind(categoryField, p -> {
            if (p.getCategoryId() != null && availableCategories != null) {
                return availableCategories.stream().filter(cat -> cat.getId().equals(p.getCategoryId())).findFirst()
                        .orElse(null);
            }
            return null;
        }, (p, value) -> {
            if (value != null) {
                p.setCategoryId(value.getId());
                p.setCategoryName(value.getName());
            } else {
                p.setCategoryId(null);
                p.setCategoryName(null);
            }
        });

        form.add(titleField, contentField, categoryField, publishedField, commentableField);

        // CPD-OFF
        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var saveButton = new Button("Save", e -> this.savePost());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var cancelButton = new Button("Cancel", e -> this.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttonLayout.add(saveButton, cancelButton);

        final var dialogLayout = new VerticalLayout(title, form, buttonLayout);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);
        dialogLayout.setSizeFull();

        this.add(dialogLayout);
        this.binder.readBean(this.currentPost);
        super.open();
        // CPD-ON
    }

    private void savePost() {
        try {
            if (!this.binder.validate().isOk()) {
                NotificationUtil.showError("Please check the form for errors");
                return;
            }

            this.binder.writeBean(this.currentPost);

            // Clear timestamp fields to let the backend handle them
            this.currentPost.setCreated(null);
            this.currentPost.setLastEdit(null);

            if (this.currentPost.getId() == null) {
                this.postService.createPost(this.currentPost);
                NotificationUtil.showSuccess("Post created successfully");
            } else {
                this.postService.updatePost(this.currentPost);
                NotificationUtil.showSuccess("Post updated successfully");
            }

            this.close();
            this.onSave.run();

        } catch (final ValidationException e) {
            NotificationUtil.showError("Please check the form for errors");
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.onAuthFailure.run();
        } catch (final ServiceException e) {
            NotificationUtil.showError(e, "Saving post failed. Please try again.");
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error saving post", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }
}
