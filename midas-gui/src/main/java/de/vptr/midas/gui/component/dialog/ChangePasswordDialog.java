package de.vptr.midas.gui.component.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.data.validator.StringLengthValidator;

import de.vptr.midas.gui.dto.PasswordChangeDto;
import de.vptr.midas.gui.service.UserService;
import de.vptr.midas.gui.util.NotificationUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Dialog for changing the current user's password.
 */
public class ChangePasswordDialog extends FormDialog {

    private final transient UserService userService;
    private final Binder<PasswordChangeDto> binder = new Binder<>(PasswordChangeDto.class);

    private final PasswordField currentPasswordField = new PasswordField("Current Password");
    private final PasswordField newPasswordField = new PasswordField("New Password");
    private final PasswordField confirmPasswordField = new PasswordField("Confirm New Password");

    /**
     * Creates a new ChangePasswordDialog.
     *
     * @param userService
     *            the user service
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Services are singleton beans")
    public ChangePasswordDialog(final UserService userService) {
        super("400px");
        this.userService = userService;
        this.setHeaderTitle("Change Password");

        this.initFields();
        this.add(this.createFormLayout());
        this.getFooter().add(this.createButtonLayout());
    }

    private void initFields() {
        this.currentPasswordField.setRequired(true);
        this.newPasswordField.setRequired(true);
        this.confirmPasswordField.setRequired(true);

        this.binder.forField(this.currentPasswordField).asRequired("Current password is required")
                .bind(PasswordChangeDto::getCurrentPassword, PasswordChangeDto::setCurrentPassword);

        this.binder.forField(this.newPasswordField).asRequired("New password is required")
                .withValidator(new StringLengthValidator("Password must be at least 8 characters long", 8, 100))
                .withValidator(value -> value.matches(".*[0-9].*"), "Must contain at least one digit")
                .withValidator(value -> value.matches(".*[a-z].*"), "Must contain at least one lowercase letter")
                .withValidator(value -> value.matches(".*[A-Z].*"), "Must contain at least one uppercase letter")
                .withValidator(value -> value.matches(".*[@#$%^&+=!].*"),
                        "Must contain at least one special character (@#$%^&+=!)")
                .bind(PasswordChangeDto::getNewPassword, PasswordChangeDto::setNewPassword);

        this.binder.forField(this.confirmPasswordField).asRequired("Please confirm your new password")
                .withValidator((value, context) -> {
                    if (value.equals(this.newPasswordField.getValue())) {
                        return ValidationResult.ok();
                    }
                    return ValidationResult.error("Passwords do not match");
                }).bind(ignore -> "", (ignore, value) -> {
                });
    }

    private FormLayout createFormLayout() {
        final var formLayout = new FormLayout();
        formLayout.add(this.currentPasswordField, this.newPasswordField, this.confirmPasswordField);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        return formLayout;
    }

    private HorizontalLayout createButtonLayout() {
        final var saveButton = new Button("Change Password", ignore -> this.save());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var cancelButton = new Button("Cancel", ignore -> this.close());

        return new HorizontalLayout(saveButton, cancelButton);
    }

    private void save() {
        final var dto = new PasswordChangeDto();
        if (this.binder.writeBeanIfValid(dto)) {
            try {
                this.userService.changePassword(dto);
                NotificationUtil.showSuccess("Password changed successfully");
                this.close();
            } catch (final Exception e) {
                NotificationUtil.showError("Failed to change password: " + e.getMessage());
            }
        }
    }
}
