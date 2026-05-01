package de.vptr.midas.gui.component.dialog;

import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import de.vptr.midas.gui.dto.UserDto;
import de.vptr.midas.gui.dto.UserGroupResponseDto;
import de.vptr.midas.gui.dto.UserResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.UserGroupService;
import de.vptr.midas.gui.service.UserService;
import de.vptr.midas.gui.util.NotificationUtil;

/**
 * Dialog panel for managing user group membership.
 * Shows a grid of current members and allows adding/removing users.
 */
public class AdminUserGroupMemberPanel extends FormDialog {

    private static final Logger LOG = Logger.getLogger(AdminUserGroupMemberPanel.class);

    private final UserGroupService groupService;
    private final UserService userService;
    private final Runnable onAuthFailure;

    private Grid<UserDto> userGrid;
    private ComboBox<UserResponseDto> availableUsersCombo;
    private UserGroupResponseDto selectedGroup;

    /** Initializes AdminUserGroupMemberPanel. */
    public AdminUserGroupMemberPanel(final UserGroupService groupService,
            final UserService userService,
            final AuthService authService,
            final Runnable onAuthFailure) {
        super();
        this.groupService = groupService;
        this.userService = userService;
        this.onAuthFailure = onAuthFailure;
    }

    /** Opens the member management dialog for the given group. */
    public void open(final UserGroupResponseDto group) {
        this.selectedGroup = group;
        this.removeAll();

        final var title = new H3("Manage Users for Group: " + group.getName());

        this.userGrid = new Grid<>(UserDto.class, false);
        this.userGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.userGrid.setHeight("300px");

        this.userGrid.addColumn((ValueProvider<UserDto, ?>) UserDto::getId)
                .setHeader("ID").setWidth("80px").setFlexGrow(0);
        this.userGrid.addColumn((ValueProvider<UserDto, ?>) UserDto::getUsername)
                .setHeader("Username").setFlexGrow(1);
        this.userGrid.addColumn(user -> user.getEmail() != null ? user.getEmail() : "no email")
                .setHeader("Email").setFlexGrow(1);

        this.userGrid.addComponentColumn(user -> {
            final var removeButton = new RemoveUserButton(_ -> this.removeUserFromGroup(user));
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

        final var addButton = new Button("Add User", _ -> this.addUserToGroup());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        addUserLayout.add(this.availableUsersCombo, addButton);

        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var refreshButton = new RefreshButton(_ -> this.loadGroupUsers());
        final var closeButton = new Button("Close", _ -> this.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttonLayout.add(refreshButton, closeButton);

        final var dialogLayout = new VerticalLayout(title, this.userGrid, addUserLayout, buttonLayout);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);
        dialogLayout.setSizeFull();

        this.add(dialogLayout);

        this.loadGroupUsers();
        this.loadAvailableUsers();

        super.open();
    }

    private void loadGroupUsers() {
        try {
            final var users = this.groupService.getUsersInGroup(this.selectedGroup.getId());
            this.userGrid.setItems(users);
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.onAuthFailure.run();
        } catch (final ServiceException e) {
            NotificationUtil.showError(e, "Loading group users failed. Please try again.");
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error loading group users", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void loadAvailableUsers() {
        try {
            final var allUsers = this.userService.getAllUsers(0, 100);

            final var currentUsers = this.userGrid.getDataProvider().fetch(new Query<>())
                    .collect(Collectors.toList());
            final var currentUserIds = currentUsers.stream()
                    .map(UserDto::getId)
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

    private void addUserToGroup() {
        final var selectedUser = this.availableUsersCombo.getValue();
        if (selectedUser == null) {
            NotificationUtil.showWarning("Please select a user to add");
            return;
        }

        final var currentUsers = this.userGrid.getDataProvider().fetch(new Query<>())
                .collect(Collectors.toList());
        final boolean alreadyInGroup = currentUsers.stream()
                .anyMatch(user -> user.getId().equals(selectedUser.getId()));

        if (alreadyInGroup) {
            NotificationUtil.showWarning("User is already a member of this group");
            this.availableUsersCombo.clear();
            return;
        }

        try {
            if (this.groupService.addUserToGroup(this.selectedGroup.getId(), selectedUser.getId())) {
                NotificationUtil.showSuccess("User added to group successfully");
                this.loadGroupUsers();
                this.loadAvailableUsers();
                this.availableUsersCombo.clear();
            } else {
                NotificationUtil.showError("Failed to add user to group");
            }
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.onAuthFailure.run();
        } catch (final ServiceException e) {
            NotificationUtil.showError(e, "Adding user to group failed. Please try again.");
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error adding user to group", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void removeUserFromGroup(final UserDto user) {
        try {
            if (this.groupService.removeUserFromGroup(this.selectedGroup.getId(), user.getId())) {
                NotificationUtil.showSuccess("User removed from group successfully");
                this.loadGroupUsers();
                this.loadAvailableUsers();
            } else {
                NotificationUtil.showError("Failed to remove user from group");
            }
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.onAuthFailure.run();
        } catch (final ServiceException e) {
            NotificationUtil.showError(e, "Removing user from group failed. Please try again.");
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error removing user from group", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }
}
