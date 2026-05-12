package de.vptr.midas.gui.component.dialog;

import java.util.Objects;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;

import de.vptr.midas.gui.dto.UserGroupDto;
import de.vptr.midas.gui.dto.UserGroupResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.UserGroupService;
import de.vptr.midas.gui.util.NotificationUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;

/**
 * Dialog for creating and editing user groups in the admin view. Encapsulates form fields, binder, and save/cancel
 * logic.
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Service reference intentionally stored for runtime behavior")
public class AdminUserGroupDialog extends FormDialog {

    private static final Logger LOG = Logger.getLogger(AdminUserGroupDialog.class);

    private final transient UserGroupService groupService;
    private final Runnable onSave;
    private final Runnable onAuthFailure;

    @Nullable
    private Binder<UserGroupDto> binder;
    @Nullable
    private transient UserGroupDto currentGroup;

    /** Initializes AdminUserGroupDialog. */
    public AdminUserGroupDialog(final UserGroupService groupService, final Runnable onSave,
            final Runnable onAuthFailure) {
        super();
        this.groupService = groupService;
        this.onSave = onSave;
        this.onAuthFailure = onAuthFailure;
    }

    /** Opens the dialog for creating or editing a user group. */
    public void open(@Nullable final UserGroupResponseDto group) {
        this.removeAll();
        this.currentGroup = group != null ? group.toUserGroupDto() : new UserGroupDto();
        this.binder = new Binder<>(UserGroupDto.class);

        final var title = new H3(group != null ? "Edit Group" : "Create Group");

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        final var nameField = new TextField("Name");
        nameField.setRequired(true);
        nameField.setMaxLength(255);
        nameField.setInvalid(false);

        this.binder.forField(nameField)
                .withValidator(value -> value != null && !value.trim().isEmpty(), "Name is required")
                .bind(UserGroupDto::getName, UserGroupDto::setName);

        form.add(nameField);

        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var saveButton = new Button("Save", ignore -> this.saveGroup());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var cancelButton = new Button("Cancel", ignore -> this.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttonLayout.add(saveButton, cancelButton);

        final var dialogLayout = new VerticalLayout(title, form, buttonLayout);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);
        dialogLayout.setSizeFull();

        this.add(dialogLayout);
        this.binder.readBean(this.currentGroup);
        super.open();
    }

    private void saveGroup() {
        try {
            final var b = Objects.requireNonNull(this.binder);
            final var g = Objects.requireNonNull(this.currentGroup);

            if (!b.validate().isOk()) {
                NotificationUtil.showError("Please check the form for errors");
                return;
            }

            b.writeBean(g);

            if (g.getId() == null) {
                this.groupService.createGroup(g);
                NotificationUtil.showSuccess("Group created successfully");
            } else {
                this.groupService.updateGroup(g);
                NotificationUtil.showSuccess("Group updated successfully");
            }

            this.close();
            this.onSave.run();

        } catch (final ValidationException e) {
            NotificationUtil.showError("Please check the form for errors");
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.onAuthFailure.run();
        } catch (final ServiceException e) {
            NotificationUtil.showError(e, "Saving group failed. Please try again.");
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error saving group", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }
}
