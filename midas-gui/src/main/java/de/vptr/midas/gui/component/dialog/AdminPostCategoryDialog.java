package de.vptr.midas.gui.component.dialog;

import java.util.List;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;

import de.vptr.midas.gui.dto.PostCategoryDto;
import de.vptr.midas.gui.dto.PostCategoryResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.PostCategoryService;
import de.vptr.midas.gui.util.NotificationUtil;

/**
 * Dialog for creating and editing post categories in the admin view.
 * Encapsulates form fields, binder, parent category filtering, and save/cancel
 * logic.
 */
public class AdminPostCategoryDialog extends FormDialog {

    private static final Logger LOG = Logger.getLogger(AdminPostCategoryDialog.class);

    private final PostCategoryService categoryService;
    private final Runnable onSave;
    private final Runnable onAuthFailure;

    private Binder<PostCategoryDto> binder;
    private PostCategoryDto currentCategory;

    /** Initializes AdminPostCategoryDialog. */
    public AdminPostCategoryDialog(final PostCategoryService categoryService,
            final Runnable onSave,
            final Runnable onAuthFailure) {
        super();
        this.categoryService = categoryService;
        this.onSave = onSave;
        this.onAuthFailure = onAuthFailure;
    }

    /**
     * Opens the dialog for the given category, with available parent categories.
     */
    public void open(final PostCategoryDto category,
            final List<PostCategoryResponseDto> allCategories) {
        this.removeAll();
        this.currentCategory = category != null ? category : new PostCategoryDto();
        this.binder = new Binder<>(PostCategoryDto.class);

        final var title = new H3(category != null && category.getId() != null
                ? "Edit Category"
                : "Create Category");

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        final var nameField = new TextField("Name");
        nameField.setRequired(true);
        nameField.setMaxLength(255);
        nameField.setInvalid(false);

        final var parentField = new ComboBox<PostCategoryDto>("Parent Category");
        parentField.setItemLabelGenerator(cat -> cat != null ? cat.getName() : "N/A");
        parentField.setPlaceholder("(none)");
        parentField.setInvalid(false);
        if (allCategories != null) {
            final var availableParents = allCategories.stream()
                    .map(PostCategoryResponseDto::toPostCategoryDto)
                    .filter(cat -> category == null || !this.isDescendantOf(cat, category, allCategories))
                    .filter(cat -> category == null || !cat.getId().equals(category.getId()))
                    .toList();
            parentField.setItems(availableParents);
        }
        parentField.setClearButtonVisible(true);

        this.binder.forField(nameField)
                .withValidator(name -> name != null && !name.trim().isEmpty(), "Name is required")
                .bind(PostCategoryDto::getName, PostCategoryDto::setName);
        this.binder.bind(parentField, PostCategoryDto::getParent, PostCategoryDto::setParent);

        form.add(nameField, parentField);

        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var saveButton = new Button("Save", _ -> this.saveCategory());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var cancelButton = new Button("Cancel", _ -> this.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttonLayout.add(saveButton, cancelButton);

        final var dialogLayout = new VerticalLayout(title, form, buttonLayout);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);
        dialogLayout.setSizeFull();

        this.add(dialogLayout);
        this.binder.readBean(this.currentCategory);
        super.open();
    }

    private boolean isDescendantOf(final PostCategoryDto potential,
            final PostCategoryDto ancestor,
            final List<PostCategoryResponseDto> allCategories) {
        if (potential.getParent() == null) {
            return false;
        }
        if (potential.getParent().getId().equals(ancestor.getId())) {
            return true;
        }

        final var parent = allCategories.stream()
                .filter(cat -> cat.getId().equals(potential.getParent().getId()))
                .map(PostCategoryResponseDto::toPostCategoryDto)
                .findFirst()
                .orElse(null);

        if (parent != null) {
            return this.isDescendantOf(parent, ancestor, allCategories);
        }

        return false;
    }

    private void saveCategory() {
        try {
            this.binder.writeBean(this.currentCategory);

            if (this.currentCategory.getId() == null) {
                this.categoryService.createCategory(this.currentCategory);
                NotificationUtil.showSuccess("Category created successfully");
            } else {
                this.categoryService.updateCategory(this.currentCategory);
                NotificationUtil.showSuccess("Category updated successfully");
            }

            this.close();
            this.onSave.run();

        } catch (final ValidationException e) {
            NotificationUtil.showError("Please check the form for errors");
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.onAuthFailure.run();
        } catch (final ServiceException e) {
            NotificationUtil.showError(e, "Saving category failed. Please try again.");
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error saving category", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }
}
