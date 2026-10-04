package de.vptr.midas.gui.component.dialog;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.data.provider.CallbackDataProvider.FetchCallback;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.function.ValueProvider;

import de.vptr.midas.gui.component.button.RemoveUserButton;
import de.vptr.midas.gui.dto.AccountDto;
import de.vptr.midas.gui.dto.AccountResponseDto;
import de.vptr.midas.gui.dto.UserResponseDto;
import de.vptr.midas.gui.service.AccountService;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.UserService;
import de.vptr.midas.gui.util.NotificationUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/** Dialog panel for managing users associated with an account. */
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Service reference intentionally stored for runtime behavior")
public class AdminAccountUserPanel extends AbstractUserAssociationPanel<AccountDto, UserResponseDto> {

    private static final Logger LOG = Logger.getLogger(AdminAccountUserPanel.class);

    private final transient AccountService accountService;
    private final transient UserService userService;

    /** Creates a new AdminAccountUserPanel. */
    public AdminAccountUserPanel(final AccountService accountService, final AuthService authService,
            final UserService userService, final Runnable onAuthFailure) {
        super(onAuthFailure);
        this.accountService = accountService;
        this.userService = userService;
    }

    /** Opens the user panel for the specified account. */
    public void open(final AccountResponseDto account) {
        this.selectedEntity = AccountDto.from(account);
        this.initLayout("Manage Users for Account: " + account.getName());
    }

    @Override
    protected Grid<UserResponseDto> createGrid() {
        return new Grid<>(UserResponseDto.class, false);
    }

    @Override
    protected void configureGridColumns(final Grid<UserResponseDto> grid) {
        grid.addColumn((ValueProvider<UserResponseDto, ?>) UserResponseDto::getPublicId).setHeader("ID")
                .setWidth("80px").setFlexGrow(0);
        grid.addColumn((ValueProvider<UserResponseDto, ?>) UserResponseDto::getUsername).setHeader("Username")
                .setFlexGrow(1);
        grid.addColumn(user -> user.getEmail() != null ? user.getEmail() : "no email").setHeader("Email")
                .setFlexGrow(1);

        grid.addComponentColumn(user -> {
            final var checkbox = new Checkbox();
            checkbox.setValue(Boolean.TRUE.equals(user.getIsNegative()));
            checkbox.setLabel("Negative");
            checkbox.addValueChangeListener(
                    e -> this.updateUserMetaFlags(user, e.getValue(), Boolean.TRUE.equals(user.getCanManage())));
            return checkbox;
        }).setHeader("Negative?").setWidth("120px").setFlexGrow(0);

        grid.addComponentColumn(user -> {
            final var checkbox = new Checkbox();
            checkbox.setValue(Boolean.TRUE.equals(user.getCanManage()));
            checkbox.setLabel("Manage");
            checkbox.addValueChangeListener(
                    e -> this.updateUserMetaFlags(user, Boolean.TRUE.equals(user.getIsNegative()), e.getValue()));
            return checkbox;
        }).setHeader("Manage?").setWidth("120px").setFlexGrow(0);

        grid.addComponentColumn(user -> new RemoveUserButton(ignore -> this.removeUserFromAccount(user)))
                .setHeader("Actions").setWidth("120px").setFlexGrow(0);
    }

    @Override
    protected String getUserPublicId(final UserResponseDto item) {
        return item.getPublicId();
    }

    @Override
    protected void loadAssociatedUsers() {
        this.executeWithHandling(() -> {
            final var accountPublicId = Objects.requireNonNull(
                    Objects.requireNonNull(this.selectedEntity).getPublicId(), "selectedEntity.publicId");
            final var users = this.accountService.getAssociatedUsers(accountPublicId);
            // The generated DTO compares by value, and updateUserMetaFlags mutates rows in place, so key rows by id.
            Objects.requireNonNull(this.userGrid).setItems(users).setIdentifierProvider(UserResponseDto::getPublicId);
        }, "Loading account users failed. Please try again.");
    }

    @Override
    protected void loadAvailableUsers() {
        Objects.requireNonNull(this.availableUsersCombo)
                .setItems((FetchCallback<UserResponseDto, String>) this::fetchAvailableUsers);
    }

    private Stream<UserResponseDto> fetchAvailableUsers(final Query<UserResponseDto, String> query) {
        try {
            final var filter = query.getFilter().orElse("").trim();
            final var currentUsers = Objects.requireNonNull(this.userGrid).getDataProvider().fetch(new Query<>())
                    .collect(Collectors.toList());
            final var currentUserIds =
                    currentUsers.stream().map(UserResponseDto::getPublicId).collect(Collectors.toSet());

            final var users = filter.isEmpty()
                    ? this.userService.getAllUsers(query.getOffset() / query.getLimit(), query.getLimit())
                    : this.userService.searchUsers(filter);

            return users.stream().filter(user -> !currentUserIds.contains(user.getPublicId())).skip(query.getOffset())
                    .limit(query.getLimit());
        } catch (final RuntimeException e) {
            LOG.error("Error loading available users", e);
            return Stream.empty();
        }
    }

    @Override
    protected void addUser() {
        final var maybeUser = this.acquireSelectedUser("User is already associated with this account");
        if (maybeUser.isEmpty()) {
            return;
        }
        final var selectedUser = maybeUser.get();
        final var combo = Objects.requireNonNull(this.availableUsersCombo);

        this.executeWithHandling(() -> {
            final var accountPublicId = Objects.requireNonNull(
                    Objects.requireNonNull(this.selectedEntity).getPublicId(), "selectedEntity.publicId");
            this.accountService.associateUserWithAccount(accountPublicId, selectedUser.getPublicId());
            NotificationUtil.showSuccess("User added to account successfully");
            this.loadAssociatedUsers();
            this.loadAvailableUsers();
            combo.clear();
        }, "Adding user to account failed. Please try again.");
    }

    private void updateUserMetaFlags(final UserResponseDto user, final boolean isNegative, final boolean canManage) {
        this.executeWithHandling(() -> {
            final var accountPublicId = Objects.requireNonNull(
                    Objects.requireNonNull(this.selectedEntity).getPublicId(), "selectedEntity.publicId");
            this.accountService.updateUserAccountMeta(accountPublicId, user.getPublicId(), isNegative, canManage);
            user.setIsNegative(isNegative);
            user.setCanManage(canManage);
            NotificationUtil.showSuccess("Updated meta flags for " + user.getUsername());
        }, "Failed to update meta flags. Please try again.");
    }

    private void removeUserFromAccount(final UserResponseDto user) {
        this.executeWithHandling(() -> {
            final var accountPublicId = Objects.requireNonNull(
                    Objects.requireNonNull(this.selectedEntity).getPublicId(), "selectedEntity.publicId");
            this.accountService.removeUserFromAccount(accountPublicId, user.getPublicId());
            NotificationUtil.showSuccess("User removed from account successfully");
            this.loadAssociatedUsers();
            this.loadAvailableUsers();
        }, "Removing user from account failed. Please try again.");
    }
}
