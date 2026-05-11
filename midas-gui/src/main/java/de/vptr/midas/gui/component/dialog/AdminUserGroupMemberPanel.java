package de.vptr.midas.gui.component.dialog;

import java.util.Objects;
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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;

/**
 * Dialog panel for managing user group membership. Shows a grid of current members and allows adding/removing users.
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Service reference intentionally stored for runtime behavior")
public class AdminUserGroupMemberPanel extends FormDialog {

    private static final Logger LOG = Logger.getLogger(AdminUserGroupMemberPanel.class);

    private final transient UserGroupService groupService;
    private final transient UserService userService;
    private final Runnable onAuthFailure;

    @Nullable
    private Grid<UserDto> userGrid;
    @Nullable
    private ComboBox<UserResponseDto> availableUsersCombo;
    @Nullable
    private UserGroupResponseDto selectedGroup;

    /** Initializes AdminUserGroupMemberPanel. */
    public AdminUserGroupMemberPanel(final UserGroupService groupService, final UserService userService,
            final AuthService authService, final Runnable onAuthFailure) {
        super();
        this.groupService = groupService;
        this.userService = userService;
        this.onAuthFailure = onAuthFailure;
    }

    /** Opens the member management dialog for the given group. */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Group reference intentionally stored for use in panel methods")
    public void open(@Nullable final UserGroupResponseDto group) {
        if (group == null) {
            return;
        }
        this.selectedGroup = group;
        this.removeAll();

        final var title = new H3("Manage Users for Group: " + group.getName());

        this.userGrid = new Grid<>(UserDto.class, false);
        this.userGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.userGrid.setHeight("300px");

        this.userGrid.addColumn((ValueProvider<UserDto, ?>) UserDto::getId).setHeader("ID").setWidth("80px")
                .setFlexGrow(0);
        this.userGrid.addColumn((ValueProvider<UserDto, ?>) UserDto::getUsername).setHeader("Username").setFlexGrow(1);
        this.userGrid.addColumn(user -> user.getEmail() != null ? user.getEmail() : "no email").setHeader("Email")
                .setFlexGrow(1);

        this.userGrid.addComponentColumn(user -> {
            final var removeButton = new RemoveUserButton(e -> this.removeUserFromGroup(user));
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

        final var addButton = new Button("Add User", e -> this.addUserToGroup());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        addUserLayout.add(this.availableUsersCombo, addButton);

        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var refreshButton = new RefreshButton(e -> this.loadGroupUsers());
        final var closeButton = new Button("Close", e -> this.close());
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
            final var users = this.groupService.getUsersInGroup(Objects.requireNonNull(this.selectedGroup).getId());
            Objects.requireNonNull(this.userGrid).setItems(users);
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

            final var currentUsers = Objects.requireNonNull(this.userGrid).getDataProvider().fetch(new Query<>())
                    .collect(Collectors.toList());
            final var currentUserIds = currentUsers.stream().map(UserDto::getId).collect(Collectors.toSet());

            final var availableUsers = allUsers.stream().filter(user -> !currentUserIds.contains(user.getId()))
                    .collect(Collectors.toList());

            Objects.requireNonNull(this.availableUsersCombo).setItems(availableUsers);
        } catch (final RuntimeException e) {
            LOG.error("Error loading available users", e);
            NotificationUtil.showError("Error loading available users");
        }
    }

    private void addUserToGroup() {
        final var combo = Objects.requireNonNull(this.availableUsersCombo);
        final var selectedUser = combo.getValue();
        if (selectedUser == null) {
            NotificationUtil.showWarning("Please select a user to add");
            return;
        }

        final var currentUsers = Objects.requireNonNull(this.userGrid).getDataProvider().fetch(new Query<>())
                .collect(Collectors.toList());
        final boolean alreadyInGroup =
                currentUsers.stream().anyMatch(user -> Objects.equals(user.getId(), selectedUser.getId()));

        if (alreadyInGroup) {
            NotificationUtil.showWarning("User is already a member of this group");
            Objects.requireNonNull(this.availableUsersCombo).clear();
            return;
        }

        try {
            if (this.groupService.addUserToGroup(Objects.requireNonNull(this.selectedGroup).getId(),
                    selectedUser.getId())) {
                NotificationUtil.showSuccess("User added to group successfully");
                this.loadGroupUsers();
                this.loadAvailableUsers();
                Objects.requireNonNull(this.availableUsersCombo).clear();
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
            if (this.groupService.removeUserFromGroup(Objects.requireNonNull(this.selectedGroup).getId(),
                    Objects.requireNonNull(user.getId()))) {
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
