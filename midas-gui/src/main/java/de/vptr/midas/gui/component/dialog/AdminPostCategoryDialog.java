package de.vptr.midas.gui.component.dialog;

import java.util.List;
import java.util.Objects;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.data.binder.Binder;

import de.vptr.midas.gui.dto.PostCategoryDto;
import de.vptr.midas.gui.dto.PostCategoryResponseDto;
import de.vptr.midas.gui.service.PostCategoryService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;

/**
 * Dialog for creating and editing post categories in the admin view. Encapsulates form fields, binder, parent category
 * filtering, and save/cancel logic.
 */
@SuppressWarnings("NullAway")
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Service reference intentionally stored for runtime behavior")
public class AdminPostCategoryDialog extends AbstractAdminEntityDialogBase<PostCategoryDto> {

    private final transient PostCategoryService categoryService;

    /** Initializes AdminPostCategoryDialog. */
    public AdminPostCategoryDialog(final PostCategoryService categoryService, final Runnable onSave,
            final Runnable onAuthFailure) {
        super(onSave, onAuthFailure);
        this.categoryService = categoryService;
    }

    /**
     * Opens the dialog for the given category, with available parent categories.
     */
    public void open(@Nullable final PostCategoryDto category, final List<PostCategoryResponseDto> allCategories) {
        this.removeAll();
        this.currentBean = category != null ? category : new PostCategoryDto();
        this.binder = new Binder<>(PostCategoryDto.class);

        final var form = this.createResponsiveFormLayout();
        final var nameField = this.createAndBindRequiredNameField(PostCategoryDto::getName, PostCategoryDto::setName);

        final var parentField = new ComboBox<PostCategoryDto>("Parent Category");
        parentField.setItemLabelGenerator(cat -> cat != null ? cat.getName() : "N/A");
        parentField.setPlaceholder("(none)");
        parentField.setInvalid(false);
        if (allCategories != null) {
            final var availableParents = allCategories.stream().map(PostCategoryResponseDto::toPostCategoryDto)
                    .filter(cat -> category == null || !this.isDescendantOf(cat, category, allCategories))
                    .filter(cat -> category == null || !cat.getPublicId().equals(category.getPublicId())).toList();
            parentField.setItems(availableParents);
        }
        parentField.setClearButtonVisible(true);

        this.binder.bind(parentField, PostCategoryDto::getParent, PostCategoryDto::setParent);

        form.add(nameField, parentField);

        final var title = category != null && category.getPublicId() != null ? "Edit Category" : "Create Category";
        this.initDialogLayout(title, form);
    }

    private boolean isDescendantOf(final PostCategoryDto potential, final PostCategoryDto ancestor,
            final List<PostCategoryResponseDto> allCategories) {
        if (potential.getParent() == null) {
            return false;
        }
        if (potential.getParent().getPublicId().equals(ancestor.getPublicId())) {
            return true;
        }

        final var parent =
                allCategories.stream().filter(cat -> cat.getPublicId().equals(potential.getParent().getPublicId()))
                        .map(PostCategoryResponseDto::toPostCategoryDto).findFirst().orElse(null);

        return parent != null && this.isDescendantOf(parent, ancestor, allCategories);
    }

    @Override
    protected void handleSave() {
        final boolean isNew = Objects.requireNonNull(this.currentBean).getPublicId() == null;
        this.executeSaveWorkflow(isNew, "Category created successfully", "Category updated successfully",
                this.categoryService::createCategory, this.categoryService::updateCategory, "Category");
    }
}
