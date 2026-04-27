package de.vptr.midas.gui.component.dialog;

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;

import de.vptr.midas.gui.dto.UserRankDto;
import de.vptr.midas.gui.dto.UserRankResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.UserRankService;
import de.vptr.midas.gui.util.NotificationUtil;

/**
 * Dialog for creating and editing user ranks in the admin view.
 * Encapsulates form fields, binder, permission checkboxes, and save/cancel
 * logic.
 */
public class AdminUserRankDialog extends FormDialog {

    private static final Logger LOG = Logger.getLogger(AdminUserRankDialog.class);

    private final UserRankService rankService;
    private final Runnable onSave;
    private final Runnable onAuthFailure;

    private Binder<UserRankDto> binder;
    private UserRankDto currentRank;

    /** Creates a new instance. */
    public AdminUserRankDialog(final UserRankService rankService,
            final Runnable onSave,
            final Runnable onAuthFailure) {
        super();
        this.rankService = rankService;
        this.onSave = onSave;
        this.onAuthFailure = onAuthFailure;
    }

    /** Opens the dialog for creating or editing a rank. */
    public void open(final UserRankResponseDto rank) {
        this.removeAll();
        this.currentRank = rank != null ? rank.toUserRankDto() : new UserRankDto();
        this.binder = new Binder<>(UserRankDto.class);

        final var title = new H3(rank != null ? "Edit Rank" : "Create Rank");

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        final var nameField = new TextField("Name");
        nameField.setRequired(true);
        nameField.setMaxLength(255);
        nameField.setInvalid(false);

        this.binder.forField(nameField)
                .withValidator(value -> value != null && !value.trim().isEmpty(), "Name is required")
                .bind(UserRankDto::getName, UserRankDto::setName);

        form.add(nameField);

        // View permissions
        final var adminViewField = new Checkbox("Admin View");
        this.bindCheckbox(adminViewField, UserRankDto::getAdminView, UserRankDto::setAdminView);

        form.add(new H3("View Permissions"));
        form.add(adminViewField);

        this.addPermissionSection(form, "Page Permissions",
                "Can Add Pages", UserRankDto::getPageAdd, UserRankDto::setPageAdd,
                "Can Edit Pages", UserRankDto::getPageEdit, UserRankDto::setPageEdit,
                "Can Delete Pages", UserRankDto::getPageDelete, UserRankDto::setPageDelete);

        this.addPermissionSection(form, "Post Permissions",
                "Can Add Posts", UserRankDto::getPostAdd, UserRankDto::setPostAdd,
                "Can Edit Posts", UserRankDto::getPostEdit, UserRankDto::setPostEdit,
                "Can Delete Posts", UserRankDto::getPostDelete, UserRankDto::setPostDelete);

        this.addPermissionSection(form, "Post Category Permissions",
                "Can Add Post Categories", UserRankDto::getPostCategoryAdd, UserRankDto::setPostCategoryAdd,
                "Can Edit Post Categories", UserRankDto::getPostCategoryEdit, UserRankDto::setPostCategoryEdit,
                "Can Delete Post Categories", UserRankDto::getPostCategoryDelete, UserRankDto::setPostCategoryDelete);

        this.addPermissionSection(form, "Post Comment Permissions",
                "Can Add Post Comments", UserRankDto::getPostCommentAdd, UserRankDto::setPostCommentAdd,
                "Can Edit Post Comments", UserRankDto::getPostCommentEdit, UserRankDto::setPostCommentEdit,
                "Can Delete Post Comments", UserRankDto::getPostCommentDelete, UserRankDto::setPostCommentDelete);

        this.addPermissionSection(form, "User Permissions",
                "Can Add Users", UserRankDto::getUserAdd, UserRankDto::setUserAdd,
                "Can Edit Users", UserRankDto::getUserEdit, UserRankDto::setUserEdit,
                "Can Delete Users", UserRankDto::getUserDelete, UserRankDto::setUserDelete);

        this.addPermissionSection(form, "User Group Permissions",
                "Can Add User Groups", UserRankDto::getUserGroupAdd, UserRankDto::setUserGroupAdd,
                "Can Edit User Groups", UserRankDto::getUserGroupEdit, UserRankDto::setUserGroupEdit,
                "Can Delete User Groups", UserRankDto::getUserGroupDelete, UserRankDto::setUserGroupDelete);

        this.addPermissionSection(form, "Account Permissions",
                "Can Add User Accounts", UserRankDto::getUserAccountAdd, UserRankDto::setUserAccountAdd,
                "Can Edit User Accounts", UserRankDto::getUserAccountEdit, UserRankDto::setUserAccountEdit,
                "Can Delete User Accounts", UserRankDto::getUserAccountDelete, UserRankDto::setUserAccountDelete);

        this.addPermissionSection(form, "User Rank Permissions",
                "Can Add User Ranks", UserRankDto::getUserRankAdd, UserRankDto::setUserRankAdd,
                "Can Edit User Ranks", UserRankDto::getUserRankEdit, UserRankDto::setUserRankEdit,
                "Can Delete User Ranks", UserRankDto::getUserRankDelete, UserRankDto::setUserRankDelete);

        form.getStyle().set("overflow-y", "auto");
        form.setMaxHeight("400px");

        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var saveButton = new Button("Save", _ -> this.saveRank());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var cancelButton = new Button("Cancel", _ -> this.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttonLayout.add(saveButton, cancelButton);

        final var dialogLayout = new VerticalLayout(title, form, buttonLayout);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);
        dialogLayout.setSizeFull();

        this.add(dialogLayout);
        this.binder.readBean(this.currentRank);
        super.open();
    }

    private void addPermissionSection(final FormLayout form, final String sectionTitle,
            final String addLabel, final Function<UserRankDto, Boolean> addGetter,
            final BiConsumer<UserRankDto, Boolean> addSetter,
            final String editLabel, final Function<UserRankDto, Boolean> editGetter,
            final BiConsumer<UserRankDto, Boolean> editSetter,
            final String deleteLabel, final Function<UserRankDto, Boolean> deleteGetter,
            final BiConsumer<UserRankDto, Boolean> deleteSetter) {
        final var addField = new Checkbox(addLabel);
        final var editField = new Checkbox(editLabel);
        final var deleteField = new Checkbox(deleteLabel);

        this.bindCheckbox(addField, addGetter, addSetter);
        this.bindCheckbox(editField, editGetter, editSetter);
        this.bindCheckbox(deleteField, deleteGetter, deleteSetter);

        form.add(new H3(sectionTitle));
        form.add(addField, editField, deleteField);
    }

    private void bindCheckbox(final Checkbox checkbox,
            final Function<UserRankDto, Boolean> getter,
            final BiConsumer<UserRankDto, Boolean> setter) {
        this.binder.bind(checkbox,
                rank -> {
                    final Boolean value = getter.apply(rank);
                    return value != null ? value : false;
                },
                setter::accept);
    }

    private void saveRank() {
        try {
            if (!this.binder.validate().isOk()) {
                NotificationUtil.showError("Please check the form for errors");
                return;
            }

            this.binder.writeBean(this.currentRank);

            if (this.currentRank.getId() == null) {
                this.rankService.createRank(this.currentRank);
                NotificationUtil.showSuccess("Rank created successfully");
            } else {
                this.rankService.updateRank(this.currentRank);
                NotificationUtil.showSuccess("Rank updated successfully");
            }

            this.close();
            this.onSave.run();

        } catch (final ValidationException e) {
            NotificationUtil.showError("Please check the form for errors");
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.onAuthFailure.run();
        } catch (final ServiceException e) {
            NotificationUtil.showError(e, "Saving rank failed. Please try again.");
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error saving rank", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }
}
