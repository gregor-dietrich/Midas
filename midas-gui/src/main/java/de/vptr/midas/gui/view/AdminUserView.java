package de.vptr.midas.gui.view;

import java.util.List;
import java.util.Objects;

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
import de.vptr.midas.gui.util.MidasErrorHandler;
import de.vptr.midas.gui.util.NotificationUtil;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** Admin view for managing application users. */
@Route(value = "admin/users", layout = AdminMainLayout.class)
public class AdminUserView extends AbstractAsyncDataView implements BeforeEnterObserver {

    private static final Logger LOG = Logger.getLogger(AdminUserView.class);

    @Inject
    transient UserService userService;

    @Inject
    transient AuthService authService;

    @Inject
    transient UserRankService userRankService;

    @Nullable
    private Grid<UserResponseDto> grid;
    @Nullable
    private TextField searchField;
    @Nullable
    private Button searchButton;

    @Nullable
    private AdminUserDialog userDialog;
    @Nullable
    private List<UserRankResponseDto> availableRanks;
    private boolean initialized = false;

    // CPD-OFF
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
        this.setupLazyDataProvider();
    }
    // CPD-ON

    private void setupLazyDataProvider() {
        Objects.requireNonNull(this.grid)
                .setItems(query -> this.userService.getAllUsers(query.getPage(), query.getPageSize()).stream());
    }

    private void loadRanksAsync() {
        LOG.info("Starting async rank loading");

        this.loadDataAsync(() -> this.userRankService.getAllRanks(), ranks -> {
            LOG.infof("Successfully loaded %s ranks", ranks.size());
            this.availableRanks = ranks;
        }, throwable -> {
            LOG.error("Error loading ranks", throwable);
            MidasErrorHandler.handleInUiContext(throwable);
        });
    }

    // CPD-OFF
    private void buildUi() {
        this.removeAll();

        final var header = new H2("Users");
        final var searchLayout = this.createSearchLayout();
        final var buttonLayout = this.createButtonLayout();
        this.createGrid();

        final Runnable authFailure = () -> this.getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        this.userDialog = new AdminUserDialog(this.userService, this::setupLazyDataProvider, authFailure);

        this.add(header, searchLayout, buttonLayout, Objects.requireNonNull(this.grid));
    }

    private HorizontalLayout createSearchLayout() {
        final var searchLayout = new SearchLayout(e -> {
            if (e.getValue() == null || e.getValue().trim().isEmpty()) {
                this.setupLazyDataProvider();
            }
        }, e -> this.searchUsers(), "Search by username or email...", "Search Users");

        this.searchButton = searchLayout.getButton();
        this.searchField = searchLayout.getTextfield();

        return searchLayout;
    }

    private HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var createButton = new CreateButton(e -> Objects.requireNonNull(this.userDialog).open(null,
                this.availableRanks != null ? this.availableRanks : List.of()));
        final var refreshButton = new RefreshButton(e -> this.setupLazyDataProvider());

        layout.add(createButton, refreshButton);
        return layout;
    }
    // CPD-ON

    private void createGrid() {
        this.grid = new Grid<>(UserResponseDto.class, false);
        this.grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.grid.setSizeFull();

        // Configure columns
        // CPD-OFF
        this.grid.addColumn((ValueProvider<UserResponseDto, ?>) UserResponseDto::getId).setHeader("ID").setWidth("80px")
                .setFlexGrow(0);

        // Make the username column clickable
        this.grid.addComponentColumn(user -> {
            final var usernameSpan = new Span(user.getUsername());
            usernameSpan.getStyle().set("color", "var(--lumo-primary-text-color)");
            usernameSpan.getStyle().set("cursor", "pointer");
            usernameSpan.getStyle().set("width", "100%");
            usernameSpan.getStyle().set("display", "block");
            usernameSpan.addClickListener(e -> Objects.requireNonNull(this.userDialog).open(user,
                    this.availableRanks != null ? this.availableRanks : List.of()));
            return usernameSpan;
        }).setHeader("Username").setFlexGrow(1);
        // CPD-ON

        this.grid.addColumn((ValueProvider<UserResponseDto, ?>) UserResponseDto::getEmail).setHeader("Email")
                .setFlexGrow(1);
        this.grid.addColumn(user -> user.getRankName() != null ? user.getRankName() : "").setHeader("Rank")
                .setWidth("120px").setFlexGrow(0);
        this.grid
                .addColumn(LitRenderer
                        .<UserResponseDto>of("<vaadin-checkbox disabled ?checked='${item.checked}'></vaadin-checkbox>")
                        .withProperty("checked", user -> Boolean.TRUE.equals(user.isActivated())))
                .setHeader("Activated").setWidth("100px").setFlexGrow(0);
        this.grid
                .addColumn(LitRenderer
                        .<UserResponseDto>of("<vaadin-checkbox disabled ?checked='${item.checked}'></vaadin-checkbox>")
                        .withProperty("checked", user -> Boolean.TRUE.equals(user.isBanned())))
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

        final var editButton = new EditButton(e -> Objects.requireNonNull(this.userDialog).open(user,
                this.availableRanks != null ? this.availableRanks : List.of()));
        final var deleteButton = new DeleteButton(e -> {
            if (user != null) {
                this.deleteUser(user);
            }
        });

        layout.add(editButton, deleteButton);
        return layout;
    }

    private void deleteUser(final UserResponseDto user) {
        try {
            if (this.userService.deleteUser(Objects.requireNonNull(user.getId()))) {
                NotificationUtil.showSuccess("User deleted successfully");
                this.setupLazyDataProvider();
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

    // CPD-OFF
    private void searchUsers() {
        final String query = Objects.requireNonNull(this.searchField).getValue();
        if (query == null || query.trim().isEmpty()) {
            NotificationUtil.showWarning("Please enter a search query");
            return;
        }
        final var sb = Objects.requireNonNull(this.searchButton);
        sb.setEnabled(false);
        sb.setText("Searching...");

        this.loadDataAsync(() -> this.userService.searchUsers(query.trim()), users -> {
            sb.setEnabled(true);
            sb.setText("Search");
            Objects.requireNonNull(this.grid).setItems(users);
        }, throwable -> {
            sb.setEnabled(true);
            sb.setText("Search");
            MidasErrorHandler.handleInUiContext(throwable);
        });
    }
    // CPD-ON
}
