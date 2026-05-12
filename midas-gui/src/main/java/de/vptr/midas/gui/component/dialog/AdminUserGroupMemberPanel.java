package de.vptr.midas.gui.component.dialog;

import java.util.Objects;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.function.ValueProvider;

import de.vptr.midas.gui.component.button.RemoveUserButton;
import de.vptr.midas.gui.dto.UserDto;
import de.vptr.midas.gui.dto.UserGroupResponseDto;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.UserGroupService;
import de.vptr.midas.gui.service.UserService;
import de.vptr.midas.gui.util.NotificationUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;

/** Dialog panel for managing user group membership. */
@SuppressWarnings("NullAway")
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Service reference intentionally stored for runtime behavior")
public class AdminUserGroupMemberPanel extends AbstractUserAssociationPanel<UserGroupResponseDto, UserDto> {

    private static final Logger LOG = Logger.getLogger(AdminUserGroupMemberPanel.class);

    private final transient UserGroupService groupService;
    private final transient UserService userService;

    /** Creates a new AdminUserGroupMemberPanel. */
    public AdminUserGroupMemberPanel(final UserGroupService groupService, final UserService userService,
            final AuthService authService, final Runnable onAuthFailure) {
        super(onAuthFailure);
        this.groupService = groupService;
        this.userService = userService;
    }

    /** Opens the member panel for the selected user group. */
    public void open(@Nullable final UserGroupResponseDto group) {
        if (group == null) {
            return;
        }
        this.selectedEntity = group;
        this.initLayout("Manage Users for Group: " + group.getName());
    }

    @Override
    protected Grid<UserDto> createGrid() {
        return new Grid<>(UserDto.class, false);
    }

    @Override
    protected void configureGridColumns(final Grid<UserDto> grid) {
        grid.addColumn((ValueProvider<UserDto, ?>) UserDto::getId).setHeader("ID").setWidth("80px").setFlexGrow(0);
        grid.addColumn((ValueProvider<UserDto, ?>) UserDto::getUsername).setHeader("Username").setFlexGrow(1);
        grid.addColumn(user -> user.getEmail() != null ? user.getEmail() : "no email").setHeader("Email")
                .setFlexGrow(1);

        grid.addComponentColumn(user -> new RemoveUserButton(ignore -> this.removeUserFromGroup(user)))
                .setHeader("Actions").setWidth("120px").setFlexGrow(0);
    }

    @Override
    protected Long getUserId(final UserDto item) {
        return item.getId();
    }

    @Override
    protected void loadAssociatedUsers() {
        this.executeWithHandling(() -> {
            final var users = this.groupService.getUsersInGroup(Objects.requireNonNull(this.selectedEntity).getId());
            Objects.requireNonNull(this.userGrid).setItems(users);
        }, "Loading group users failed. Please try again.");
    }

    @Override
    protected void loadAvailableUsers() {
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

    @Override
    protected void addUser() {
        final var combo = Objects.requireNonNull(this.availableUsersCombo);
        final var selectedUser = combo.getValue();
        if (selectedUser == null) {
            NotificationUtil.showWarning("Please select a user to add");
            return;
        }

        if (this.isUserAssociated(selectedUser.getId())) {
            NotificationUtil.showWarning("User is already a member of this group");
            combo.clear();
            return;
        }

        this.executeWithHandling(() -> {
            if (this.groupService.addUserToGroup(Objects.requireNonNull(this.selectedEntity).getId(),
                    selectedUser.getId())) {
                NotificationUtil.showSuccess("User added to group successfully");
                this.loadAssociatedUsers();
                this.loadAvailableUsers();
                combo.clear();
            } else {
                NotificationUtil.showError("Failed to add user to group");
            }
        }, "Adding user to group failed. Please try again.");
    }

    private void removeUserFromGroup(final UserDto user) {
        this.executeWithHandling(() -> {
            if (this.groupService.removeUserFromGroup(Objects.requireNonNull(this.selectedEntity).getId(),
                    Objects.requireNonNull(user.getId()))) {
                NotificationUtil.showSuccess("User removed from group successfully");
                this.loadAssociatedUsers();
                this.loadAvailableUsers();
            } else {
                NotificationUtil.showError("Failed to remove user from group");
            }
        }, "Removing user from group failed. Please try again.");
    }
}
