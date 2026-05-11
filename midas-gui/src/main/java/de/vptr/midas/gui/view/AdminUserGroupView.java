package de.vptr.midas.gui.view;

import java.util.Objects;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.component.button.CreateButton;
import de.vptr.midas.gui.component.button.DeleteButton;
import de.vptr.midas.gui.component.button.EditButton;
import de.vptr.midas.gui.component.button.ManageUsersButton;
import de.vptr.midas.gui.component.button.RefreshButton;
import de.vptr.midas.gui.component.dialog.AdminUserGroupDialog;
import de.vptr.midas.gui.component.dialog.AdminUserGroupMemberPanel;
import de.vptr.midas.gui.component.layout.IntegerFilterLayout;
import de.vptr.midas.gui.component.layout.SearchLayout;
import de.vptr.midas.gui.dto.UserGroupResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.UserGroupService;
import de.vptr.midas.gui.service.UserService;
import de.vptr.midas.gui.util.MidasErrorHandler;
import de.vptr.midas.gui.util.NotificationUtil;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** Admin view for managing user groups. */
@Route(value = "admin/user-groups", layout = AdminMainLayout.class)
public class AdminUserGroupView extends AbstractAsyncDataView implements BeforeEnterObserver {

    private static final Logger LOG = Logger.getLogger(AdminUserGroupView.class);

    @Inject
    UserGroupService groupService;

    @Inject
    AuthService authService;

    @Inject
    UserService userService;

    @Nullable
    private Grid<UserGroupResponseDto> grid;
    @Nullable
    private TextField searchField;
    @Nullable
    private Button searchButton;
    @Nullable
    private IntegerField userIdField;

    @Nullable
    private AdminUserGroupDialog groupDialog;
    @Nullable
    private AdminUserGroupMemberPanel memberPanel;

    /** Initializes AdminUserGroupView. */
    public AdminUserGroupView() {
        this.setSizeFull();
        this.setPadding(true);
        this.setSpacing(true);
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        try {
            this.authService.requireAuthorizationHeader();
        } catch (final AuthenticationException e) {
            LOG.debugf("Authentication required: %s", e.getMessage());
            event.forwardTo(LoginView.class);
            return;
        }

        this.buildUi();
        this.setupLazyDataProvider();
    }

    private void setupLazyDataProvider() {
        Objects.requireNonNull(this.grid)
                .setItems(query -> this.groupService.getAllGroups(query.getPage(), query.getPageSize()).stream());
    }

    private void buildUi() {
        this.removeAll();

        final Runnable onAuthFailure = () -> this.getUI().ifPresent(ui -> ui.navigate(LoginView.class));

        final var header = new H2("User Groups");
        final var searchLayout = this.createSearchLayout();
        final var buttonLayout = this.createButtonLayout();
        this.createGrid();
        this.groupDialog = new AdminUserGroupDialog(this.groupService, this::setupLazyDataProvider, onAuthFailure);
        this.memberPanel =
                new AdminUserGroupMemberPanel(this.groupService, this.userService, this.authService, onAuthFailure);

        this.add(header, searchLayout, buttonLayout, Objects.requireNonNull(this.grid));
    }

    private HorizontalLayout createSearchLayout() {
        final var searchLayout = new SearchLayout(e -> {
            if (e.getValue() == null || e.getValue().trim().isEmpty()) {
                this.setupLazyDataProvider();
            }
        }, _ -> this.searchGroups(), "Search by name...", "Search Groups");

        this.searchButton = searchLayout.getButton();
        this.searchField = searchLayout.getTextfield();

        // User ID filter
        final var userFilterLayout =
                new IntegerFilterLayout(_ -> this.filterByUser(), "Enter User ID...", "Filter by User");
        this.userIdField = userFilterLayout.getIntegerField();

        searchLayout.add(userFilterLayout);
        return searchLayout;
    }

    private HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var createButton = new CreateButton(_ -> Objects.requireNonNull(this.groupDialog).open(null));
        final var refreshButton = new RefreshButton(_ -> this.setupLazyDataProvider());

        layout.add(createButton, refreshButton);
        return layout;
    }

    private void createGrid() {
        this.grid = new Grid<>(UserGroupResponseDto.class, false);
        this.grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.grid.setSizeFull();

        // Configure columns
        this.grid.addColumn((ValueProvider<UserGroupResponseDto, ?>) UserGroupResponseDto::getId).setHeader("ID")
                .setWidth("80px").setFlexGrow(0);

        // Make the name column clickable
        this.grid.addComponentColumn(group -> {
            final var nameSpan = new Span(group.getName());
            nameSpan.getStyle().set("color", "var(--lumo-primary-text-color)");
            nameSpan.getStyle().set("cursor", "pointer");
            nameSpan.getStyle().set("width", "100%");
            nameSpan.getStyle().set("display", "block");
            nameSpan.addClickListener(_ -> Objects.requireNonNull(this.groupDialog).open(group));
            return nameSpan;
        }).setHeader("Name").setFlexGrow(2);

        this.grid.addColumn(group -> group.getUserCount() != null ? group.getUserCount() : 0).setHeader("User Count")
                .setWidth("120px").setFlexGrow(0);

        // Add action column
        this.grid.addComponentColumn(this::createActionButtons).setHeader("Actions").setWidth("180px").setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(final UserGroupResponseDto group) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var editButton = new EditButton(_ -> Objects.requireNonNull(this.groupDialog).open(group));
        final var deleteButton = new DeleteButton(_ -> this.deleteGroup(group));
        final var manageUsersButton = new ManageUsersButton(_ -> Objects.requireNonNull(this.memberPanel).open(group));

        layout.add(editButton, deleteButton, manageUsersButton);
        return layout;
    }

    private void deleteGroup(final UserGroupResponseDto group) {
        try {
            if (this.groupService.deleteGroup(Objects.requireNonNull(group.getId()))) {
                NotificationUtil.showSuccess("Group deleted successfully");
                this.setupLazyDataProvider();
            } else {
                NotificationUtil.showError("Failed to delete group");
            }
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        } catch (final ServiceException e) {
            NotificationUtil.showError(e, "Deleting group failed. Please try again.");
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error deleting group", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void searchGroups() {
        final String query = Objects.requireNonNull(this.searchField).getValue();
        if (query == null || query.trim().isEmpty()) {
            this.setupLazyDataProvider();
            return;
        }

        final var sb = Objects.requireNonNull(this.searchButton);
        sb.setEnabled(false);

        this.loadDataAsync(() -> this.groupService.searchGroups(query), result -> {
            sb.setEnabled(true);
            Objects.requireNonNull(this.grid).setItems(result);
        }, throwable -> {
            sb.setEnabled(true);
            MidasErrorHandler.handleInUiContext(throwable);
        });
    }

    private void filterByUser() {
        final Integer userId = Objects.requireNonNull(this.userIdField).getValue();
        if (userId == null) {
            this.setupLazyDataProvider();
            return;
        }

        this.loadDataAsync(() -> this.groupService.getGroupsByUser(userId.longValue()),
                result -> Objects.requireNonNull(this.grid).setItems(result));
    }
}
