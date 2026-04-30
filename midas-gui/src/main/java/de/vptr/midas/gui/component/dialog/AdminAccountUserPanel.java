package de.vptr.midas.gui.component.dialog;

import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.function.ValueProvider;

import de.vptr.midas.gui.component.button.RefreshButton;
import de.vptr.midas.gui.component.button.RemoveUserButton;
import de.vptr.midas.gui.dto.AccountDto;
import de.vptr.midas.gui.dto.AccountResponseDto;
import de.vptr.midas.gui.dto.UserResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.AccountService;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.UserService;
import de.vptr.midas.gui.util.NotificationUtil;

/**
 * Dialog panel for managing users associated with an account.
 * Shows a grid of current users with remove capability and a combo to add new
 * users.
 */
public class AdminAccountUserPanel extends FormDialog {

    private static final Logger LOG = Logger.getLogger(AdminAccountUserPanel.class);

    private final AccountService accountService;
    private final AuthService authService;
    private final UserService userService;
    private final Runnable onAuthFailure;

    private Grid<UserResponseDto> userGrid;
    private ComboBox<UserResponseDto> availableUsersCombo;
    private AccountDto selectedAccount;

    /** Initializes AdminAccountUserPanel. */
    public AdminAccountUserPanel(final AccountService accountService,
            final AuthService authService,
            final UserService userService,
            final Runnable onAuthFailure) {
        super();
        this.accountService = accountService;
        this.authService = authService;
        this.userService = userService;
        this.onAuthFailure = onAuthFailure;
    }

    /** Opens the user management dialog for the given account. */
    public void open(final AccountResponseDto account) {
        this.selectedAccount = account.toAccountDto();
        this.removeAll();

        final var title = new H3("Manage Users for Account: " + account.getName());

        this.userGrid = new Grid<>(UserResponseDto.class, false);
        this.userGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.userGrid.setHeight("300px");

        this.userGrid.addColumn((ValueProvider<UserResponseDto, ?>) UserResponseDto::getId).setHeader("ID")
                .setWidth("80px").setFlexGrow(0);
        this.userGrid.addColumn((ValueProvider<UserResponseDto, ?>) UserResponseDto::getUsername).setHeader("Username")
                .setFlexGrow(1);
        this.userGrid.addColumn(user -> user.getEmail() != null ? user.getEmail() : "no email").setHeader("Email")
                .setFlexGrow(1);

        this.userGrid.addComponentColumn(user -> {
            final var checkbox = new Checkbox();
            checkbox.setValue(user.isNegative());
            checkbox.setLabel("Negative");
            checkbox.addValueChangeListener(e -> this.updateUserNegativeFlag(user, e.getValue()));
            return checkbox;
        }).setHeader("Negative?").setWidth("120px").setFlexGrow(0);

        this.userGrid.addComponentColumn(user -> {
            final var removeButton = new RemoveUserButton(_ -> this.removeUserFromAccount(user));
            return removeButton;
        }).setHeader("Actions").setWidth("120px").setFlexGrow(0);

        final var addUserLayout = new HorizontalLayout();
        addUserLayout.setAlignItems(VerticalLayout.Alignment.END);
        addUserLayout.setSpacing(true);

        this.availableUsersCombo = new ComboBox<>("Add User");
        this.availableUsersCombo.setItemLabelGenerator(
                user -> user.getUsername() + " (" + (user.getEmail() != null ? user.getEmail() : "no email") + ")");
        this.availableUsersCombo.setWidth("300px");
        this.availableUsersCombo.setPlaceholder("Select a user to add...");

        final var addButton = new Button("Add User", _ -> this.addUserToAccount());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        addUserLayout.add(this.availableUsersCombo, addButton);

        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var refreshButton = new RefreshButton(_ -> this.loadAccountUsers());
        final var closeButton = new Button("Close", _ -> this.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttonLayout.add(refreshButton, closeButton);

        final var dialogLayout = new VerticalLayout(title, this.userGrid, addUserLayout, buttonLayout);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);
        dialogLayout.setSizeFull();

        this.add(dialogLayout);

        this.loadAccountUsers();
        this.loadAvailableUsers();

        super.open();
    }

    private void loadAccountUsers() {
        try {
            final var users = this.accountService.getAssociatedUsers(this.selectedAccount.getId());
            this.userGrid.setItems(users);
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.onAuthFailure.run();
        } catch (final ServiceException e) {
            NotificationUtil.showError(e, "Loading account users failed. Please try again.");
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error loading account users", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void loadAvailableUsers() {
        try {
            final String authHeader;
            try {
                authHeader = this.authService.getAuthorizationHeader();
            } catch (final RuntimeException e) {
                LOG.error("Failed to get auth header", e);
                NotificationUtil.showError("Authentication failed");
                return;
            }

            final var allUsers = this.userService.getAllUsers(authHeader, 0, 100);

            final var currentUsers = this.userGrid.getDataProvider().fetch(new Query<>())
                    .collect(Collectors.toList());
            final var currentUserIds = currentUsers.stream()
                    .map(UserResponseDto::getId)
                    .collect(Collectors.toSet());

            final var availableUsers = allUsers.stream()
                    .filter(user -> !currentUserIds.contains(user.getId()))
                    .collect(Collectors.toList());

            this.availableUsersCombo.setItems(availableUsers);
        } catch (final RuntimeException e) {
            LOG.error("Error loading available users", e);
            NotificationUtil.showError("Error loading available users");
        }
    }

    private void addUserToAccount() {
        final var selectedUser = this.availableUsersCombo.getValue();
        if (selectedUser == null) {
            NotificationUtil.showWarning("Please select a user to add");
            return;
        }

        final var currentUsers = this.userGrid.getDataProvider().fetch(new Query<>())
                .collect(Collectors.toList());
        final boolean alreadyAssociated = currentUsers.stream()
                .anyMatch(user -> user.getId().equals(selectedUser.getId()));

        if (alreadyAssociated) {
            NotificationUtil.showWarning("User is already associated with this account");
            this.availableUsersCombo.clear();
            return;
        }

        try {
            if (this.accountService.associateUserWithAccount(this.selectedAccount.getId(), selectedUser.getId())) {
                NotificationUtil.showSuccess("User added to account successfully");
                this.loadAccountUsers();
                this.loadAvailableUsers();
                this.availableUsersCombo.clear();
            } else {
                NotificationUtil.showError("Failed to add user to account");
            }
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.onAuthFailure.run();
        } catch (final ServiceException e) {
            NotificationUtil.showError(e, "Adding user to account failed. Please try again.");
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error adding user to account", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void updateUserNegativeFlag(final UserResponseDto user, final boolean isNegative) {
        try {
            this.accountService.updateUserAccountMeta(
                    this.selectedAccount.getId(), user.getId(), isNegative);
            NotificationUtil.showSuccess("Updated negative flag for " + user.getUsername());
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.onAuthFailure.run();
        } catch (final ServiceException e) {
            NotificationUtil.showError(e, "Failed to update negative flag. Please try again.");
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error updating negative flag", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void removeUserFromAccount(final UserResponseDto user) {
        try {
            if (this.accountService.removeUserFromAccount(this.selectedAccount.getId(), user.getId())) {
                NotificationUtil.showSuccess("User removed from account successfully");
                this.loadAccountUsers();
                this.loadAvailableUsers();
            } else {
                NotificationUtil.showError("Failed to remove user from account");
            }
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.onAuthFailure.run();
        } catch (final ServiceException e) {
            NotificationUtil.showError(e, "Removing user from account failed. Please try again.");
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error removing user from account", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }
}
