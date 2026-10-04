package de.vptr.midas.gui.component.dialog;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.data.binder.Binder;

import de.vptr.midas.gui.dto.UserRankDto;
import de.vptr.midas.gui.dto.UserRankResponseDto;
import de.vptr.midas.gui.service.UserRankService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;

/**
 * Dialog for creating and editing user ranks in the admin view. Encapsulates form fields, binder, permission
 * checkboxes, and save/cancel logic.
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Service reference intentionally stored for runtime behavior")
public class AdminUserRankDialog extends AbstractAdminEntityDialogBase<UserRankDto> {

    private final transient UserRankService rankService;

    /** Initializes AdminUserRankDialog. */
    public AdminUserRankDialog(final UserRankService rankService, final Runnable onSave, final Runnable onAuthFailure) {
        super(onSave, onAuthFailure);
        this.rankService = rankService;
    }

    /** Opens the dialog for creating or editing a rank. */
    public void open(@Nullable final UserRankResponseDto rank) {
        this.removeAll();
        this.currentBean = rank != null ? UserRankDto.from(rank) : new UserRankDto();
        this.binder = new Binder<>(UserRankDto.class);

        final var form = this.createResponsiveFormLayout();
        final var nameField = this.createAndBindRequiredNameField(UserRankDto::getName, UserRankDto::setName);

        form.add(nameField);

        final var adminViewField = new Checkbox("Admin View");
        this.bindCheckbox(adminViewField, UserRankDto::isAdminView, UserRankDto::setAdminView);

        form.add(new H3("View Permissions"));
        form.add(adminViewField);

        this.addPermissionSection(form, "Page Permissions", "Can Add Pages", UserRankDto::isPageAdd,
                UserRankDto::setPageAdd, "Can Edit Pages", UserRankDto::isPageEdit, UserRankDto::setPageEdit,
                "Can Delete Pages", UserRankDto::isPageDelete, UserRankDto::setPageDelete);

        this.addPermissionSection(form, "Post Permissions", "Can Add Posts", UserRankDto::isPostAdd,
                UserRankDto::setPostAdd, "Can Edit Posts", UserRankDto::isPostEdit, UserRankDto::setPostEdit,
                "Can Delete Posts", UserRankDto::isPostDelete, UserRankDto::setPostDelete);

        this.addPermissionSection(form, "Post Category Permissions", "Can Add Post Categories",
                UserRankDto::isPostCategoryAdd, UserRankDto::setPostCategoryAdd, "Can Edit Post Categories",
                UserRankDto::isPostCategoryEdit, UserRankDto::setPostCategoryEdit, "Can Delete Post Categories",
                UserRankDto::isPostCategoryDelete, UserRankDto::setPostCategoryDelete);

        this.addPermissionSection(form, "Post Comment Permissions", "Can Add Post Comments",
                UserRankDto::isPostCommentAdd, UserRankDto::setPostCommentAdd, "Can Edit Post Comments",
                UserRankDto::isPostCommentEdit, UserRankDto::setPostCommentEdit, "Can Delete Post Comments",
                UserRankDto::isPostCommentDelete, UserRankDto::setPostCommentDelete);

        this.addPermissionSection(form, "User Permissions", "Can Add Users", UserRankDto::isUserAdd,
                UserRankDto::setUserAdd, "Can Edit Users", UserRankDto::isUserEdit, UserRankDto::setUserEdit,
                "Can Delete Users", UserRankDto::isUserDelete, UserRankDto::setUserDelete);

        this.addPermissionSection(form, "User Group Permissions", "Can Add User Groups", UserRankDto::isUserGroupAdd,
                UserRankDto::setUserGroupAdd, "Can Edit User Groups", UserRankDto::isUserGroupEdit,
                UserRankDto::setUserGroupEdit, "Can Delete User Groups", UserRankDto::isUserGroupDelete,
                UserRankDto::setUserGroupDelete);

        this.addPermissionSection(form, "Account Permissions", "Can Add User Accounts", UserRankDto::isUserAccountAdd,
                UserRankDto::setUserAccountAdd, "Can Edit User Accounts", UserRankDto::isUserAccountEdit,
                UserRankDto::setUserAccountEdit, "Can Delete User Accounts", UserRankDto::isUserAccountDelete,
                UserRankDto::setUserAccountDelete);

        this.addPermissionSection(form, "User Rank Permissions", "Can Add User Ranks", UserRankDto::isUserRankAdd,
                UserRankDto::setUserRankAdd, "Can Edit User Ranks", UserRankDto::isUserRankEdit,
                UserRankDto::setUserRankEdit, "Can Delete User Ranks", UserRankDto::isUserRankDelete,
                UserRankDto::setUserRankDelete);

        form.getStyle().set("overflow-y", "auto");
        form.setMaxHeight("400px");

        this.initDialogLayout(rank != null ? "Edit Rank" : "Create Rank", form);
    }

    private void addPermissionSection(final FormLayout form, final String sectionTitle, final String addLabel,
            final Function<UserRankDto, Boolean> addGetter, final BiConsumer<UserRankDto, Boolean> addSetter,
            final String editLabel, final Function<UserRankDto, Boolean> editGetter,
            final BiConsumer<UserRankDto, Boolean> editSetter, final String deleteLabel,
            final Function<UserRankDto, Boolean> deleteGetter, final BiConsumer<UserRankDto, Boolean> deleteSetter) {
        final var addField = new Checkbox(addLabel);
        final var editField = new Checkbox(editLabel);
        final var deleteField = new Checkbox(deleteLabel);

        this.bindCheckbox(addField, addGetter, addSetter);
        this.bindCheckbox(editField, editGetter, editSetter);
        this.bindCheckbox(deleteField, deleteGetter, deleteSetter);

        form.add(new H3(sectionTitle));
        form.add(addField, editField, deleteField);
    }

    private void bindCheckbox(final Checkbox checkbox, final Function<UserRankDto, Boolean> getter,
            final BiConsumer<UserRankDto, Boolean> setter) {
        Objects.requireNonNull(this.binder).bind(checkbox, rank -> {
            final Boolean value = getter.apply(rank);
            return value != null ? value : false;
        }, setter::accept);
    }

    @Override
    protected void handleSave() {
        final boolean isNew = Objects.requireNonNull(this.currentBean).getPublicId() == null;
        this.executeSaveWorkflow(isNew, "Rank created successfully", "Rank updated successfully",
                this.rankService::createRank, this.rankService::updateRank, "Rank");
    }
}
