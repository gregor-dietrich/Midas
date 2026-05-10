package de.vptr.midas.gui.view;

import java.time.format.DateTimeFormatter;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.component.layout.AsyncSearchLayout;
import de.vptr.midas.gui.dto.UserResponseDto;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.UserService;
import de.vptr.midas.gui.util.MidasErrorHandler;
import de.vptr.midas.gui.util.UiConstants;
import jakarta.inject.Inject;

/** User view for browsing users. */
@Route(value = "users", layout = MainLayout.class)
public class UserView extends AbstractAsyncDataView implements BeforeEnterObserver {

    private static final Logger LOG = Logger.getLogger(UserView.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @Inject
    UserService userService;

    @Inject
    AuthService authService;

    private Grid<UserResponseDto> grid;
    private AsyncSearchLayout searchLayout;

    /** Initializes UserView. */
    public UserView() {
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

        this.buildUi();
        this.loadUsersAsync();
    }

    private void loadUsersAsync() {
        LOG.info("Starting async user loading");

        this.loadDataAsync(() -> this.userService.getAllUsers(0, UiConstants.DEFAULT_PAGE_SIZE), users -> {
            LOG.infof("Successfully loaded %s users", users.size());
            this.grid.setItems(users);
        });
    }

    private void buildUi() {
        this.removeAll();

        final var header = new H2("Users");
        this.searchLayout =
                new AsyncSearchLayout(this::searchUsers, this::loadUsersAsync, "Search by username...", "Search Users");
        this.createGrid();

        this.add(header, this.searchLayout, this.grid);
    }

    private void createGrid() {
        this.grid = new Grid<>(UserResponseDto.class, false);
        this.grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.grid.setSizeFull();
        this.grid.setPageSize(20);

        // Clickable username column that navigates to detail view
        this.grid.addComponentColumn(user -> {
            final var usernameSpan = new Span(user.getUsername());
            usernameSpan.getStyle().set("color", "var(--lumo-primary-text-color)");
            usernameSpan.getStyle().set("cursor", "pointer");
            usernameSpan.getStyle().set("width", "100%");
            usernameSpan.getStyle().set("display", "block");
            usernameSpan.addClickListener(e -> this.getUI().ifPresent(ui -> ui.navigate("users/" + user.getId())));
            return usernameSpan;
        }).setHeader("Username").setFlexGrow(1);

        this.grid.addColumn(user -> {
            final var created = user.getCreated();
            return created != null ? created.format(DATE_FORMATTER) : "";
        }).setHeader("Member Since").setFlexGrow(1);
    }

    private void searchUsers(final String query) {
        this.searchLayout.setSearching(true);

        this.loadDataAsync(() -> this.userService.searchUsers(query), users -> {
            this.searchLayout.setSearching(false);
            LOG.infof("Search found %s users", users.size());
            this.grid.setItems(users);
        }, throwable -> {
            this.searchLayout.setSearching(false);
            MidasErrorHandler.handleInUiContext(throwable);
        });
    }
}
