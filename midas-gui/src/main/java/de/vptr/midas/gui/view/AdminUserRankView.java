package de.vptr.midas.gui.view;

import java.util.Objects;
import java.util.function.Function;

import org.jboss.logging.Logger;
import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.component.button.CreateButton;
import de.vptr.midas.gui.component.button.DeleteButton;
import de.vptr.midas.gui.component.button.EditButton;
import de.vptr.midas.gui.component.button.RefreshButton;
import de.vptr.midas.gui.component.dialog.AdminUserRankDialog;
import de.vptr.midas.gui.component.layout.SearchLayout;
import de.vptr.midas.gui.dto.UserRankResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.UserRankService;
import de.vptr.midas.gui.util.MidasErrorHandler;
import de.vptr.midas.gui.util.NotificationUtil;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** Admin view for managing user ranks and permissions. */
@Route(value = "admin/user-ranks", layout = AdminMainLayout.class)
public class AdminUserRankView extends AbstractAsyncDataView implements BeforeEnterObserver {

    private static final Logger LOG = Logger.getLogger(AdminUserRankView.class);

    @Inject
    transient UserRankService rankService;

    @Inject
    transient AuthService authService;

    @Nullable
    private Grid<UserRankResponseDto> grid;
    @Nullable
    private TextField searchField;
    @Nullable
    private Button searchButton;

    @Nullable
    private AdminUserRankDialog rankDialog;
    private boolean initialized = false;

    /** Initializes AdminUserRankView. */
    public AdminUserRankView() {
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
    }

    private void loadRanksAsync() {
        LOG.info("Starting async rank loading");

        this.loadDataAsync(() -> this.rankService.getAllRanks(), ranks -> {
            LOG.infof("Successfully loaded %s ranks", ranks.size());
            Objects.requireNonNull(this.grid).setItems(ranks);
        });
    }

    private void buildUi() {
        this.removeAll();

        final var header = new H2("User Ranks");
        final var searchLayout = this.createSearchLayout();
        final var buttonLayout = this.createButtonLayout();
        this.createGrid();

        final Runnable authFailure = () -> this.getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        this.rankDialog = new AdminUserRankDialog(this.rankService, this::loadRanksAsync, authFailure);

        this.add(header, searchLayout, buttonLayout, Objects.requireNonNull(this.grid));
    }

    private HorizontalLayout createSearchLayout() {
        final var searchLayout = new SearchLayout(e -> {
            if (e.getValue() == null || e.getValue().trim().isEmpty()) {
                this.loadRanksAsync();
            }
        }, ignore -> this.searchRanks(), "Search by name...", "Search Ranks");

        this.searchButton = searchLayout.getButton();
        this.searchField = searchLayout.getTextfield();

        return searchLayout;
    }

    private HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var createButton = new CreateButton(ignore -> Objects.requireNonNull(this.rankDialog).open(null));
        final var refreshButton = new RefreshButton(ignore -> this.loadRanksAsync());

        layout.add(createButton, refreshButton);
        return layout;
    }

    private void createGrid() {
        this.grid = new Grid<>(UserRankResponseDto.class, false);
        this.grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.grid.setSizeFull();

        this.grid.addColumn(UserRankResponseDto::getId).setHeader("ID").setWidth("80px").setFlexGrow(0);

        this.grid.addComponentColumn(rank -> {
            final var nameSpan = new Span(rank.getName());
            nameSpan.getStyle().set("color", "var(--lumo-primary-text-color)");
            nameSpan.getStyle().set("cursor", "pointer");
            nameSpan.getStyle().set("width", "100%");
            nameSpan.getStyle().set("display", "block");
            nameSpan.addClickListener(ignore -> Objects.requireNonNull(this.rankDialog).open(rank));
            return nameSpan;
        }).setHeader("Name").setFlexGrow(2);

        // Admin view column (unique pattern: check/ban icons)
        this.grid.addComponentColumn(rank -> {
            final var layout = new HorizontalLayout();
            layout.setSpacing(true);
            layout.setPadding(false);

            final SvgIcon icon = Boolean.TRUE.equals(rank.isAdminView()) ? LineAwesomeIcon.CHECK_SOLID.create()
                    : LineAwesomeIcon.BAN_SOLID.create();
            icon.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
            icon.setTooltipText("Admin View");

            layout.add(icon);
            return layout;
        }).setHeader("Admin View").setWidth("110px").setFlexGrow(0);

        // CRUD permission columns
        this.addCrudPermissionColumn("Pages", UserRankResponseDto::isPageAdd, "Add Pages",
                UserRankResponseDto::isPageEdit, "Edit Pages", UserRankResponseDto::isPageDelete, "Delete Pages");

        this.addCrudPermissionColumn("Posts", UserRankResponseDto::isPostAdd, "Add Posts",
                UserRankResponseDto::isPostEdit, "Edit Posts", UserRankResponseDto::isPostDelete, "Delete Posts");

        this.addCrudPermissionColumn("Post Categories", UserRankResponseDto::isPostCategoryAdd, "Add Post Categories",
                UserRankResponseDto::isPostCategoryEdit, "Edit Post Categories",
                UserRankResponseDto::isPostCategoryDelete, "Delete Post Categories");

        this.addCrudPermissionColumn("Post Comments", UserRankResponseDto::isPostCommentAdd, "Add Comments",
                UserRankResponseDto::isPostCommentEdit, "Edit Comments", UserRankResponseDto::isPostCommentDelete,
                "Delete Comments");

        this.addCrudPermissionColumn("Users", UserRankResponseDto::isUserAdd, "Add Users",
                UserRankResponseDto::isUserEdit, "Edit Users", UserRankResponseDto::isUserDelete, "Delete Users");

        this.addCrudPermissionColumn("User Groups", UserRankResponseDto::isUserGroupAdd, "Add User Groups",
                UserRankResponseDto::isUserGroupEdit, "Edit User Groups", UserRankResponseDto::isUserGroupDelete,
                "Delete User Groups");

        this.addCrudPermissionColumn("Accounts", UserRankResponseDto::isUserAccountAdd, "Add Accounts",
                UserRankResponseDto::isUserAccountEdit, "Edit Accounts", UserRankResponseDto::isUserAccountDelete,
                "Delete Accounts");

        this.addCrudPermissionColumn("User Ranks", UserRankResponseDto::isUserRankAdd, "Add Ranks",
                UserRankResponseDto::isUserRankEdit, "Edit Ranks", UserRankResponseDto::isUserRankDelete,
                "Delete Ranks");

        this.grid.addComponentColumn(this::createActionButtons).setHeader("Actions").setWidth("150px").setFlexGrow(0);
    }

    private void addCrudPermissionColumn(final String header, final Function<UserRankResponseDto, Boolean> canAdd,
            final String addTooltip, final Function<UserRankResponseDto, Boolean> canEdit, final String editTooltip,
            final Function<UserRankResponseDto, Boolean> canDelete, final String deleteTooltip) {
        Objects.requireNonNull(this.grid).addComponentColumn(rank -> {
            final var layout = new HorizontalLayout();
            layout.setSpacing(true);
            layout.setPadding(false);
            layout.add(this.createPermissionIcon(canAdd.apply(rank), LineAwesomeIcon.PLUS_SOLID, addTooltip),
                    this.createPermissionIcon(canEdit.apply(rank), LineAwesomeIcon.EDIT_SOLID, editTooltip),
                    this.createPermissionIcon(canDelete.apply(rank), LineAwesomeIcon.TRASH_ALT_SOLID, deleteTooltip));
            return layout;
        }).setHeader(header).setWidth("150px").setFlexGrow(0);
    }

    private Component createPermissionIcon(final Boolean hasPermission, final LineAwesomeIcon icon,
            final String tooltip) {
        final Component component;
        if (Boolean.TRUE.equals(hasPermission)) {
            final SvgIcon permissionIcon = icon.create();
            permissionIcon.setTooltipText(tooltip);
            component = permissionIcon;
        } else {
            component = new Span("");
        }
        component.getElement().getStyle().set("width", "16px").set("height", "16px").set("flex-shrink", "0");
        return component;
    }

    private HorizontalLayout createActionButtons(final UserRankResponseDto rank) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var editButton = new EditButton(ignore -> Objects.requireNonNull(this.rankDialog).open(rank));
        final var deleteButton = new DeleteButton(ignore -> this.deleteRank(rank));

        layout.add(editButton, deleteButton);
        return layout;
    }

    private void deleteRank(final UserRankResponseDto rank) {
        try {
            this.rankService.deleteRank(Objects.requireNonNull(rank.getId()));
            NotificationUtil.showSuccess("Rank deleted successfully");
            this.loadRanksAsync();
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        } catch (final ServiceException e) {
            NotificationUtil.showError(e, "Deleting rank failed. Please try again.");
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error deleting rank", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void searchRanks() {
        final String query = Objects.requireNonNull(this.searchField).getValue();
        if (query == null || query.trim().isEmpty()) {
            this.loadRanksAsync();
            return;
        }

        final var sb = Objects.requireNonNull(this.searchButton);
        sb.setEnabled(false);
        LOG.infof("Starting async rank search with query: %s", query);

        this.loadDataAsync(() -> this.rankService.searchRanks(query), ranks -> {
            sb.setEnabled(true);
            LOG.infof("Successfully found %s ranks", ranks.size());
            Objects.requireNonNull(this.grid).setItems(ranks);
        }, throwable -> {
            sb.setEnabled(true);
            MidasErrorHandler.handleInUiContext(throwable);
        });
    }
}
