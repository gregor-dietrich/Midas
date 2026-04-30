package de.vptr.midas.gui.view;

import java.util.List;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.component.button.CreateButton;
import de.vptr.midas.gui.component.button.DeleteButton;
import de.vptr.midas.gui.component.button.EditButton;
import de.vptr.midas.gui.component.button.RefreshButton;
import de.vptr.midas.gui.component.dialog.AdminUserDialog;
import de.vptr.midas.gui.component.layout.SearchLayout;
import de.vptr.midas.gui.dto.UserRankResponseDto;
import de.vptr.midas.gui.dto.UserResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.UserRankService;
import de.vptr.midas.gui.service.UserService;
import de.vptr.midas.gui.util.ErrorHandler;
import de.vptr.midas.gui.util.NotificationUtil;
import de.vptr.midas.gui.util.UiConstants;
import jakarta.inject.Inject;

/** Admin view for managing application users. */
@Route(value = "admin/users", layout = AdminMainLayout.class)
public class AdminUserView extends AbstractAsyncDataView implements BeforeEnterObserver {

    private static final Logger LOG = Logger.getLogger(AdminUserView.class);

    @Inject
    UserService userService;

    @Inject
    AuthService authService;

    @Inject
    UserRankService userRankService;

    private Grid<UserResponseDto> grid;
    private TextField searchField;
    private Button searchButton;

    private AdminUserDialog userDialog;
    private List<UserRankResponseDto> availableRanks;
    private boolean initialized = false;

    /** Initializes AdminUserView. */
    public AdminUserView() {
        this.setSizeFull();
        this.setPadding(true);
        this.setSpacing(true);
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (!this.authService.isAuthenticated()) {
            event.forwardTo(LoginView.class);
            return;
        }
        if (!this.initialized) {
            this.buildUi();
            this.initialized = true;
        }
        this.loadRanksAsync();
        this.loadUsersAsync();
    }

    private void loadUsersAsync() {
        LOG.info("Starting async user loading");

        final String authHeader;
        try {
            authHeader = this.authService.getAuthorizationHeader();
        } catch (final RuntimeException e) {
            LOG.error("Failed to get auth header", e);
            this.getUI().ifPresent(ui -> ui.access(() -> {
                NotificationUtil.showError("Authentication failed");
            }));
            return;
        }

        this.loadDataAsync(
                () -> this.userService.getAllUsers(authHeader, 0, UiConstants.DEFAULT_PAGE_SIZE),
                users -> {
                    LOG.infof("Successfully loaded %s users", users.size());
                    this.grid.setItems(users);
                });
    }

    private void loadRanksAsync() {
        LOG.info("Starting async rank loading");

        final String authHeader;
        try {
            authHeader = this.authService.getAuthorizationHeader();
        } catch (final RuntimeException e) {
            LOG.error("Failed to get auth header for ranks", e);
            return;
        }

        this.loadDataAsync(
                () -> this.userRankService.getAllRanks(authHeader),
                ranks -> {
                    LOG.infof("Successfully loaded %s ranks", ranks.size());
                    this.availableRanks = ranks;
                },
                throwable -> {
                    LOG.error("Error loading ranks", throwable);
                    ErrorHandler.handleInUiContext(throwable);
                });
    }

    private void buildUi() {
        final var header = new H2("Users");
        final var searchLayout = this.createSearchLayout();
        final var buttonLayout = this.createButtonLayout();
        this.createGrid();

        final Runnable authFailure = () -> this.getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        this.userDialog = new AdminUserDialog(
                this.userService, this::loadUsersAsync, authFailure);

        this.add(header, searchLayout, buttonLayout, this.grid);
    }

    private HorizontalLayout createSearchLayout() {
        final var searchLayout = new SearchLayout(
                e -> {
                    if (e.getValue() == null || e.getValue().trim().isEmpty()) {
                        this.loadUsersAsync();
                    }
                },
                _ -> this.searchUsers(),
                "Search by username or email...",
                "Search Users");

        this.searchButton = searchLayout.getButton();
        this.searchField = searchLayout.getTextfield();

        return searchLayout;
    }

    private HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var createButton = new CreateButton(_ -> this.userDialog.open(null, this.availableRanks));
        final var refreshButton = new RefreshButton(_ -> this.loadUsersAsync());

        layout.add(createButton, refreshButton);
        return layout;
    }

    private void createGrid() {
        this.grid = new Grid<>(UserResponseDto.class, false);
        this.grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.grid.setSizeFull();

        // Configure columns
        this.grid.addColumn((ValueProvider<UserResponseDto, ?>) UserResponseDto::getId).setHeader("ID").setWidth("80px")
                .setFlexGrow(0);

        // Make the username column clickable
        this.grid.addComponentColumn(user -> {
            final var usernameSpan = new Span(user.getUsername());
            usernameSpan.getStyle().set("color", "var(--lumo-primary-text-color)");
            usernameSpan.getStyle().set("cursor", "pointer");
            usernameSpan.getStyle().set("width", "100%");
            usernameSpan.getStyle().set("display", "block");
            usernameSpan.addClickListener(_ -> this.userDialog.open(user, this.availableRanks));
            return usernameSpan;
        }).setHeader("Username").setFlexGrow(1);

        this.grid.addColumn((ValueProvider<UserResponseDto, ?>) UserResponseDto::getEmail).setHeader("Email")
                .setFlexGrow(1);
        this.grid.addColumn(user -> user.getRankName() != null ? user.getRankName() : "").setHeader("Rank")
                .setWidth("120px")
                .setFlexGrow(0);
        this.grid.addColumn(LitRenderer.<UserResponseDto>of(
                "<vaadin-checkbox disabled ?checked='${item.checked}'></vaadin-checkbox>")
                .withProperty("checked", user -> Boolean.TRUE.equals(user.getActivated())))
                .setHeader("Activated").setWidth("100px").setFlexGrow(0);
        this.grid.addColumn(LitRenderer.<UserResponseDto>of(
                "<vaadin-checkbox disabled ?checked='${item.checked}'></vaadin-checkbox>")
                .withProperty("checked", user -> Boolean.TRUE.equals(user.getBanned())))
                .setHeader("Banned").setWidth("100px").setFlexGrow(0);
        this.grid.addColumn((ValueProvider<UserResponseDto, ?>) UserResponseDto::getCreated).setHeader("Created")
                .setWidth("150px").setFlexGrow(0);
        this.grid.addColumn((ValueProvider<UserResponseDto, ?>) UserResponseDto::getLastLogin).setHeader("Last Login")
                .setWidth("150px").setFlexGrow(0);

        // Add action column
        this.grid.addComponentColumn(this::createActionButtons).setHeader("Actions").setWidth("180px").setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(final UserResponseDto user) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var editButton = new EditButton(_ -> this.userDialog.open(user, this.availableRanks));
        final var deleteButton = new DeleteButton(_ -> this.deleteUser(user));

        layout.add(editButton, deleteButton);
        return layout;
    }

    private void deleteUser(final UserResponseDto user) {
        try {
            if (this.userService.deleteUser(user.getId())) {
                NotificationUtil.showSuccess("User deleted successfully");
                this.loadUsersAsync();
            } else {
                NotificationUtil.showError("Failed to delete user");
            }
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        } catch (final ServiceException e) {
            NotificationUtil.showError(e, "Deleting user failed. Please try again.");
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error deleting user", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void searchUsers() {
        final String query = this.searchField.getValue();
        if (query == null || query.trim().isEmpty()) {
            NotificationUtil.showWarning("Please enter a search query");
            return;
        }
        final String authHeader;
        try {
            authHeader = this.authService.getAuthorizationHeader();
        } catch (final RuntimeException e) {
            NotificationUtil.showError("Authentication failed");
            return;
        }
        this.searchButton.setEnabled(false);
        this.searchButton.setText("Searching...");

        this.loadDataAsync(
                () -> this.userService.searchUsers(query.trim(), authHeader),
                users -> {
                    this.searchButton.setEnabled(true);
                    this.searchButton.setText("Search");
                    this.grid.setItems(users);
                },
                throwable -> {
                    this.searchButton.setEnabled(true);
                    this.searchButton.setText("Search");
                    ErrorHandler.handleInUiContext(throwable);
                });
    }
}
