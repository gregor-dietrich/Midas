package de.vptr.midas.gui.component.dialog;

import java.util.List;
import java.util.Objects;

import org.jboss.logging.Logger;
import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;

import de.vptr.midas.gui.dto.UserDto;
import de.vptr.midas.gui.dto.UserRankResponseDto;
import de.vptr.midas.gui.dto.UserResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.UserService;
import de.vptr.midas.gui.util.NotificationUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;

/**
 * Dialog for creating and editing users in the admin view. Encapsulates form fields, binder, password management, and
 * save/cancel logic.
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Service reference intentionally stored for runtime behavior")
public class AdminUserDialog extends FormDialog {

    private static final Logger LOG = Logger.getLogger(AdminUserDialog.class);
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 100;

    private final transient UserService userService;
    private final Runnable onSave;
    private final Runnable onAuthFailure;

    private final FormDialog passwordDialog;
    @Nullable
    private Binder<UserDto> binder;
    @Nullable
    private transient UserDto currentUser;

    /** Initializes AdminUserDialog. */
    public AdminUserDialog(final UserService userService, final Runnable onSave, final Runnable onAuthFailure) {
        super();
        this.userService = userService;
        this.onSave = onSave;
        this.onAuthFailure = onAuthFailure;
        this.passwordDialog = new FormDialog();
    }

    /** Opens the dialog. */
    public void open(@Nullable final UserResponseDto user, @Nullable final List<UserRankResponseDto> availableRanks) {
        this.removeAll();
        this.currentUser = user != null ? user.toUserDto() : new UserDto();
        this.binder = new Binder<>(UserDto.class);

        final var title = new H3(user != null ? "Edit User" : "Create User");

        final VerticalLayout titleLayout = new VerticalLayout();
        titleLayout.setPadding(false);
        titleLayout.setSpacing(false);
        titleLayout.add(title);

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        final var usernameField = new TextField("Username");
        usernameField.setRequired(true);
        usernameField.setMaxLength(255);
        usernameField.setInvalid(false);

        final var emailField = new EmailField("Email");
        emailField.setMaxLength(255);

        form.add(usernameField, emailField);

        if (user == null) {
            final var passwordField = new PasswordField("Password");
            passwordField.setRequired(true);
            passwordField.setMaxLength(MAX_PASSWORD_LENGTH);
            passwordField.setInvalid(false);
            form.add(passwordField);
            this.binder.forField(passwordField).asRequired("Password is required")
                    .withValidator(this::isValidPassword,
                            "Password must be " + MIN_PASSWORD_LENGTH + "-" + MAX_PASSWORD_LENGTH
                                    + " chars with at least one letter and one digit")
                    .bind(UserDto::getPassword, UserDto::setPassword);
        } else {
            final var passwordLayout = new VerticalLayout();
            passwordLayout.setPadding(false);
            passwordLayout.setMargin(false);
            passwordLayout.setSpacing(false);

            final var passwordLabel = new Span("Password");
            passwordLabel.getStyle().set("font-size", "var(--lumo-font-size-s)");
            passwordLabel.getStyle().set("font-weight", "500");
            passwordLabel.getStyle().set("color", "var(--lumo-secondary-text-color)");
            passwordLabel.getStyle().set("margin-bottom", "var(--lumo-space-xs)");

            final var passwordButton = new Button("Change Password");
            passwordButton.setPrefixComponent(LineAwesomeIcon.LOCK_SOLID.create());
            passwordButton.addClickListener(e -> this.openPasswordDialog(user));

            passwordLayout.add(passwordLabel, passwordButton);
            form.add(passwordLayout);
        }

        final var rankField = new ComboBox<UserRankResponseDto>("Rank");
        rankField.setItems(availableRanks != null ? availableRanks : List.of());
        rankField.setItemLabelGenerator(r -> r != null ? r.getName() : "");
        rankField.setRequired(true);
        rankField.setPlaceholder("Select a rank");
        rankField.setClearButtonVisible(false);
        rankField.setInvalid(false);

        final var activatedField = new Checkbox("Activated");
        final var bannedField = new Checkbox("Banned");

        form.add(rankField, activatedField, bannedField);

        // Bind fields with proper validation
        this.binder.forField(usernameField).asRequired("Username is required").bind(UserDto::getUsername,
                UserDto::setUsername);

        this.binder.forField(emailField).withValidator(email -> {
            if (email == null || email.trim().isEmpty()) {
                return true;
            }
            return email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
        }, "Email must be valid or empty").bind(UserDto::getEmail,
                (u, value) -> u.setEmail((value == null || value.trim().isEmpty()) ? null : value.trim()));

        this.binder.bind(activatedField, u -> Boolean.TRUE.equals(u.isActivated()), UserDto::setActivated);
        this.binder.bind(bannedField, u -> Boolean.TRUE.equals(u.isBanned()), UserDto::setBanned);

        // Rank binding - convert between UserRankResponseDto and rankPublicId
        this.binder.forField(rankField).asRequired("Rank is required").bind(u -> {
            if (u.getRankPublicId() != null && availableRanks != null) {
                return availableRanks.stream().filter(rank -> Objects.equals(rank.getPublicId(), u.getRankPublicId()))
                        .findFirst().orElse(null);
            }
            return null;
        }, (u, value) -> {
            if (value != null) {
                u.setRankPublicId(value.getPublicId());
            } else {
                u.setRankPublicId(null);
            }
        });

        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var saveButton = new Button("Save", e -> this.saveUser());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var cancelButton = new Button("Cancel", e -> this.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttonLayout.add(saveButton, cancelButton);

        final var dialogLayout = new VerticalLayout(titleLayout, form, buttonLayout);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);

        this.add(dialogLayout);
        this.binder.readBean(this.currentUser);
        super.open();
    }

    private void openPasswordDialog(final UserResponseDto user) {
        this.passwordDialog.removeAll();

        final var title = new H3("Change Password for " + user.getUsername());

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        final var newPasswordField = new PasswordField("New Password");
        newPasswordField.setRequired(true);
        newPasswordField.setMaxLength(MAX_PASSWORD_LENGTH);
        newPasswordField.setInvalid(false);

        final var confirmPasswordField = new PasswordField("Confirm Password");
        confirmPasswordField.setRequired(true);
        confirmPasswordField.setMaxLength(MAX_PASSWORD_LENGTH);
        confirmPasswordField.setInvalid(false);

        form.add(newPasswordField, confirmPasswordField);

        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var saveButton = new Button("Change Password", e -> {
            final String newPassword = newPasswordField.getValue();
            final String confirmPassword = confirmPasswordField.getValue();

            if (newPassword == null || newPassword.trim().isEmpty()) {
                NotificationUtil.showError("Password is required");
                return;
            }

            if (!this.isValidPassword(newPassword)) {
                NotificationUtil.showError("Password must be " + MIN_PASSWORD_LENGTH + "-" + MAX_PASSWORD_LENGTH
                        + " chars with at least one letter and one digit");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                NotificationUtil.showError("Passwords do not match");
                return;
            }

            this.changeUserPassword(user.getPublicId(), newPassword);
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var cancelButton = new Button("Cancel", e -> this.passwordDialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttonLayout.add(saveButton, cancelButton);

        final var dialogLayout = new VerticalLayout(title, form, buttonLayout);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);

        this.passwordDialog.add(dialogLayout);
        this.passwordDialog.open();
    }

    private void changeUserPassword(final String userId, final String newPassword) {
        try {
            final var passwordUpdateDto = new UserDto();
            passwordUpdateDto.setPublicId(userId);
            passwordUpdateDto.setPassword(newPassword);

            this.userService.updateUserPassword(passwordUpdateDto);
            NotificationUtil.showSuccess("Password changed successfully");
            this.passwordDialog.close();

        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.onAuthFailure.run();
        } catch (final ServiceException e) {
            NotificationUtil.showError(e, "Changing password failed. Please try again.");
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error changing password", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void saveUser() {
        try {
            final var b = Objects.requireNonNull(this.binder);
            final var u = Objects.requireNonNull(this.currentUser);

            b.writeBean(u);

            if (u.getPublicId() == null) {
                this.userService.createUser(u);
                NotificationUtil.showSuccess("User created successfully");
            } else {
                this.userService.updateUser(u);
                NotificationUtil.showSuccess("User updated successfully");
            }

            this.close();
            this.onSave.run();

        } catch (final ValidationException e) {
            NotificationUtil.showError("Please check the form for errors");
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.onAuthFailure.run();
        } catch (final ServiceException e) {
            NotificationUtil.showError(e, "Saving user failed. Please try again.");
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error saving user", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private boolean isValidPassword(@Nullable final String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            return false;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int i = 0; i < password.length(); i++) {
            final char c = password.charAt(i);
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
            if (hasLetter && hasDigit) {
                return true;
            }
        }
        return false;
    }
}
