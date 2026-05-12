package de.vptr.midas.gui.view;

import java.util.List;
import java.util.Objects;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.component.button.CreateButton;
import de.vptr.midas.gui.component.button.DeleteButton;
import de.vptr.midas.gui.component.button.EditButton;
import de.vptr.midas.gui.component.button.RefreshButton;
import de.vptr.midas.gui.component.dialog.AdminUserDialog;
import de.vptr.midas.gui.dto.UserRankResponseDto;
import de.vptr.midas.gui.dto.UserResponseDto;
import de.vptr.midas.gui.service.UserRankService;
import de.vptr.midas.gui.service.UserService;
import de.vptr.midas.gui.util.MidasErrorHandler;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** Admin view for managing application users. */
@Route(value = "admin/users", layout = AdminMainLayout.class)
public class AdminUserView extends AbstractAdminEntityView<UserResponseDto> {

    private static final Logger LOG = Logger.getLogger(AdminUserView.class);

    @Inject
    transient UserService userService;

    @Inject
    transient UserRankService userRankService;

    @Nullable
    private AdminUserDialog userDialog;
    @Nullable
    private List<UserRankResponseDto> availableRanks;

    @Override
    protected void onBeforeEnterAuthenticated() {
        this.loadRanksAsync();
        this.setupLazyDataProvider();
    }

    @Override
    protected String getViewTitle() {
        return "Users";
    }

    @Override
    protected Class<UserResponseDto> getEntityClass() {
        return UserResponseDto.class;
    }

    @Override
    protected String getEntityNameSingle() {
        return "User";
    }

    @Override
    protected String getSearchPlaceholder() {
        return "Search by username or email...";
    }

    @Override
    protected String getSearchButtonText() {
        return "Search Users";
    }

    @Override
    protected boolean hasDateAndUserFilters() {
        return false;
    }

    @Override
    protected void initAdditionalComponents() {
        this.userDialog =
                new AdminUserDialog(this.userService, this::setupLazyDataProvider, this.getAuthFailureRunnable());
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

    @Override
    protected void setupLazyDataProvider() {
        Objects.requireNonNull(this.grid)
                .setItems(query -> this.userService.getAllUsers(query.getPage(), query.getPageSize()).stream());
    }

    @Override
    protected void executeSearch() {
        final String query = Objects.requireNonNull(this.searchField).getValue();
        this.executeAsyncSearch(query, q -> this.userService.searchUsers(q.trim()));
    }

    @Override
    protected HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);
        layout.add(
                new CreateButton(ignore -> Objects.requireNonNull(this.userDialog).open(null,
                        this.availableRanks != null ? this.availableRanks : List.of())),
                new RefreshButton(ignore -> this.setupLazyDataProvider()));
        return layout;
    }

    @Override
    protected void configureGrid(final Grid<UserResponseDto> g) {
        g.addColumn((ValueProvider<UserResponseDto, ?>) UserResponseDto::getId).setHeader("ID").setWidth("80px")
                .setFlexGrow(0);

        g.addComponentColumn(user -> this.createClickableSpan(user.getUsername(),
                ignore -> Objects.requireNonNull(this.userDialog).open(user,
                        this.availableRanks != null ? this.availableRanks : List.of())))
                .setHeader("Username").setFlexGrow(1);

        g.addColumn((ValueProvider<UserResponseDto, ?>) UserResponseDto::getEmail).setHeader("Email").setFlexGrow(1);
        g.addColumn(user -> user.getRankName() != null ? user.getRankName() : "").setHeader("Rank").setWidth("120px")
                .setFlexGrow(0);
        g.addColumn(LitRenderer
                .<UserResponseDto>of("<vaadin-checkbox disabled ?checked='${item.checked}'></vaadin-checkbox>")
                .withProperty("checked", user -> Boolean.TRUE.equals(user.isActivated()))).setHeader("Activated")
                .setWidth("100px").setFlexGrow(0);
        g.addColumn(LitRenderer
                .<UserResponseDto>of("<vaadin-checkbox disabled ?checked='${item.checked}'></vaadin-checkbox>")
                .withProperty("checked", user -> Boolean.TRUE.equals(user.isBanned()))).setHeader("Banned")
                .setWidth("100px").setFlexGrow(0);
        g.addColumn((ValueProvider<UserResponseDto, ?>) UserResponseDto::getCreated).setHeader("Created")
                .setWidth("150px").setFlexGrow(0);
        g.addColumn((ValueProvider<UserResponseDto, ?>) UserResponseDto::getLastLogin).setHeader("Last Login")
                .setWidth("150px").setFlexGrow(0);

        g.addComponentColumn(this::createActionButtons).setHeader("Actions").setWidth("150px").setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(final UserResponseDto user) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var editButton = new EditButton(ignore -> Objects.requireNonNull(this.userDialog).open(user,
                this.availableRanks != null ? this.availableRanks : List.of()));
        final var deleteButton = new DeleteButton(ignore -> this.handleDeletion(() -> {
            this.userService.deleteUser(Objects.requireNonNull(user.getId()));
        }));

        layout.add(editButton, deleteButton);
        return layout;
    }
}
