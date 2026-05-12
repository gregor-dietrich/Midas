package de.vptr.midas.gui.component.dialog;

import java.util.Objects;

import com.vaadin.flow.data.binder.Binder;

import de.vptr.midas.gui.dto.UserGroupDto;
import de.vptr.midas.gui.dto.UserGroupResponseDto;
import de.vptr.midas.gui.service.UserGroupService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;

/**
 * Dialog for creating and editing user groups in the admin view. Encapsulates form fields, binder, and save/cancel
 * logic.
 */
@SuppressWarnings("NullAway")
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Service reference intentionally stored for runtime behavior")
public class AdminUserGroupDialog extends AbstractAdminEntityDialogBase<UserGroupDto> {

    private final transient UserGroupService groupService;

    /** Initializes AdminUserGroupDialog. */
    public AdminUserGroupDialog(final UserGroupService groupService, final Runnable onSave,
            final Runnable onAuthFailure) {
        super(onSave, onAuthFailure);
        this.groupService = groupService;
    }

    /** Opens the dialog for creating or editing a user group. */
    public void open(@Nullable final UserGroupResponseDto group) {
        this.removeAll();
        this.currentBean = group != null ? group.toUserGroupDto() : new UserGroupDto();
        this.binder = new Binder<>(UserGroupDto.class);

        final var form = this.createResponsiveFormLayout();
        final var nameField = this.createAndBindRequiredNameField(UserGroupDto::getName, UserGroupDto::setName);

        form.add(nameField);

        this.initDialogLayout(group != null ? "Edit Group" : "Create Group", form);
    }

    @Override
    protected void handleSave() {
        final boolean isNew = Objects.requireNonNull(this.currentBean).getId() == null;
        this.executeSaveWorkflow(isNew, "Group created successfully", "Group updated successfully",
                this.groupService::createGroup, this.groupService::updateGroup, "Group");
    }
}
