package de.vptr.midas.gui.view;

import java.time.LocalDate;
import java.util.Objects;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.function.ValueProvider;
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
import de.vptr.midas.gui.service.UserGroupService;
import de.vptr.midas.gui.service.UserService;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** Admin view for managing user groups. */
@Route(value = "admin/user-groups", layout = AdminMainLayout.class)
public class AdminUserGroupView extends AbstractAdminEntityView<UserGroupResponseDto> {

    @Inject
    transient UserGroupService groupService;

    @Inject
    transient UserService userService;

    @Nullable
    private AdminUserGroupDialog groupDialog;
    @Nullable
    private AdminUserGroupMemberPanel memberPanel;

    @Override
    protected void onBeforeEnterAuthenticated() {
        this.setupLazyDataProvider();
    }

    @Override
    protected String getViewTitle() {
        return "User Groups";
    }

    @Override
    protected Class<UserGroupResponseDto> getEntityClass() {
        return UserGroupResponseDto.class;
    }

    @Override
    protected String getEntityNameSingle() {
        return "Group";
    }

    @Override
    protected String getSearchPlaceholder() {
        return "Search by name...";
    }

    @Override
    protected String getSearchButtonText() {
        return "Search Groups";
    }

    @Override
    protected void addCustomFilterLayouts(final SearchLayout searchLayout) {
        final var userFilterLayout =
                new IntegerFilterLayout(ignore -> this.filterByUser(), "Enter User ID...", "Filter by User");
        this.userIdField = userFilterLayout.getIntegerField();
        searchLayout.add(userFilterLayout);
    }

    @Override
    protected void executeUserFilter(final Integer userId) {
        this.loadDataAsync(() -> this.groupService.getGroupsByUser(userId.toString()),
                result -> Objects.requireNonNull(this.grid).setItems(result));
    }

    @Override
    protected void initAdditionalComponents() {
        final Runnable onAuthFailure = this.getAuthFailureRunnable();
        this.groupDialog = new AdminUserGroupDialog(this.groupService, this::setupLazyDataProvider, onAuthFailure);
        this.memberPanel =
                new AdminUserGroupMemberPanel(this.groupService, this.userService, this.authService, onAuthFailure);
    }

    @Override
    protected void setupLazyDataProvider() {
        Objects.requireNonNull(this.grid)
                .setItems(query -> this.groupService.getAllGroups(query.getPage(), query.getPageSize()).stream());
    }

    @Override
    protected void executeSearch() {
        final String query = Objects.requireNonNull(this.searchField).getValue();
        this.executeAsyncSearch(query, q -> this.groupService.searchGroups(q.trim()));
    }

    @Override
    protected HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);
        layout.add(new CreateButton(ignore -> Objects.requireNonNull(this.groupDialog).open(null)),
                new RefreshButton(ignore -> this.setupLazyDataProvider()));
        return layout;
    }

    @Override
    protected void configureGrid(final Grid<UserGroupResponseDto> g) {
        g.addColumn((ValueProvider<UserGroupResponseDto, ?>) UserGroupResponseDto::getPublicId).setHeader("ID")
                .setWidth("80px").setFlexGrow(0);

        g.addComponentColumn(group -> this.createClickableSpan(group.getName(),
                ignore -> Objects.requireNonNull(this.groupDialog).open(group))).setHeader("Name").setFlexGrow(2);

        g.addColumn(group -> group.getUserCount() != null ? group.getUserCount() : 0).setHeader("User Count")
                .setWidth("120px").setFlexGrow(0);

        g.addComponentColumn(this::createActionButtons).setHeader("Actions").setWidth("180px").setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(final UserGroupResponseDto group) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var editButton = new EditButton(ignore -> Objects.requireNonNull(this.groupDialog).open(group));
        final var deleteButton = new DeleteButton(ignore -> this.handleDeletion(() -> {
            this.groupService.deleteGroup(Objects.requireNonNull(group.getPublicId()));
        }));
        final var manageUsersButton =
                new ManageUsersButton(ignore -> Objects.requireNonNull(this.memberPanel).open(group));

        layout.add(editButton, deleteButton, manageUsersButton);
        return layout;
    }

    @Override
    protected void executeDateRangeFilter(final LocalDate startDate, final LocalDate endDate) {
        // Not used: this view uses only user-ID filtering
    }
}
