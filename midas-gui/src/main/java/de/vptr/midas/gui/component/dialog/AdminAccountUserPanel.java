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
@SuppressWarnings("NullAway")
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
        this.selectedEntity = account.toAccountDto();
        this.initLayout("Manage Users for Account: " + account.getName());
    }

    @Override
    protected Grid<UserResponseDto> createGrid() {
        return new Grid<>(UserResponseDto.class, false);
    }

    @Override
    protected void configureGridColumns(final Grid<UserResponseDto> grid) {
        grid.addColumn((ValueProvider<UserResponseDto, ?>) UserResponseDto::getId).setHeader("ID").setWidth("80px")
                .setFlexGrow(0);
        grid.addColumn((ValueProvider<UserResponseDto, ?>) UserResponseDto::getUsername).setHeader("Username")
                .setFlexGrow(1);
        grid.addColumn(user -> user.getEmail() != null ? user.getEmail() : "no email").setHeader("Email")
                .setFlexGrow(1);

        grid.addComponentColumn(user -> {
            final var checkbox = new Checkbox();
            checkbox.setValue(user.isNegative());
            checkbox.setLabel("Negative");
            checkbox.addValueChangeListener(e -> this.updateUserNegativeFlag(user, e.getValue()));
            return checkbox;
        }).setHeader("Negative?").setWidth("120px").setFlexGrow(0);

        grid.addComponentColumn(user -> new RemoveUserButton(ignore -> this.removeUserFromAccount(user)))
                .setHeader("Actions").setWidth("120px").setFlexGrow(0);
    }

    @Override
    protected Long getUserId(final UserResponseDto item) {
        return item.getId();
    }

    @Override
    protected void loadAssociatedUsers() {
        this.executeWithHandling(() -> {
            final var users =
                    this.accountService.getAssociatedUsers(Objects.requireNonNull(this.selectedEntity).getId());
            Objects.requireNonNull(this.userGrid).setItems(users);
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
            final var currentUserIds = currentUsers.stream().map(UserResponseDto::getId).collect(Collectors.toSet());

            final var users = filter.isEmpty()
                    ? this.userService.getAllUsers(query.getOffset() / query.getLimit(), query.getLimit())
                    : this.userService.searchUsers(filter);

            return users.stream().filter(user -> !currentUserIds.contains(user.getId())).skip(query.getOffset())
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
            if (this.accountService.associateUserWithAccount(Objects.requireNonNull(this.selectedEntity).getId(),
                    selectedUser.getId())) {
                NotificationUtil.showSuccess("User added to account successfully");
                this.loadAssociatedUsers();
                this.loadAvailableUsers();
                combo.clear();
            } else {
                NotificationUtil.showError("Failed to add user to account");
            }
        }, "Adding user to account failed. Please try again.");
    }

    private void updateUserNegativeFlag(final UserResponseDto user, final boolean isNegative) {
        this.executeWithHandling(() -> {
            this.accountService.updateUserAccountMeta(Objects.requireNonNull(this.selectedEntity).getId(), user.getId(),
                    isNegative);
            NotificationUtil.showSuccess("Updated negative flag for " + user.getUsername());
        }, "Failed to update negative flag. Please try again.");
    }

    private void removeUserFromAccount(final UserResponseDto user) {
        this.executeWithHandling(() -> {
            if (this.accountService.removeUserFromAccount(Objects.requireNonNull(this.selectedEntity).getId(),
                    user.getId())) {
                NotificationUtil.showSuccess("User removed from account successfully");
                this.loadAssociatedUsers();
                this.loadAvailableUsers();
            } else {
                NotificationUtil.showError("Failed to remove user from account");
            }
        }, "Removing user from account failed. Please try again.");
    }
}
