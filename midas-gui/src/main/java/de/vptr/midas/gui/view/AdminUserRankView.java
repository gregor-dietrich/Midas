package de.vptr.midas.gui.view;

import java.util.Objects;
import java.util.function.Function;

import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.component.button.CreateButton;
import de.vptr.midas.gui.component.button.DeleteButton;
import de.vptr.midas.gui.component.button.EditButton;
import de.vptr.midas.gui.component.button.RefreshButton;
import de.vptr.midas.gui.component.dialog.AdminUserRankDialog;
import de.vptr.midas.gui.dto.UserRankResponseDto;
import de.vptr.midas.gui.service.UserRankService;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** Admin view for managing user ranks and permissions. */
@Route(value = "admin/user-ranks", layout = AdminMainLayout.class)
public class AdminUserRankView extends AbstractAdminEntityView<UserRankResponseDto> {

    @Inject
    transient UserRankService rankService;

    @Nullable
    private AdminUserRankDialog rankDialog;

    @Override
    protected void onBeforeEnterAuthenticated() {
        this.setupLazyDataProvider();
    }

    @Override
    protected String getViewTitle() {
        return "User Ranks";
    }

    @Override
    protected Class<UserRankResponseDto> getEntityClass() {
        return UserRankResponseDto.class;
    }

    @Override
    protected String getEntityNameSingle() {
        return "Rank";
    }

    @Override
    protected String getSearchPlaceholder() {
        return "Search by name...";
    }

    @Override
    protected String getSearchButtonText() {
        return "Search Ranks";
    }

    @Override
    protected boolean hasDateAndUserFilters() {
        return false;
    }

    @Override
    protected void initAdditionalComponents() {
        this.rankDialog =
                new AdminUserRankDialog(this.rankService, this::setupLazyDataProvider, this.getAuthFailureRunnable());
    }

    @Override
    protected void setupLazyDataProvider() {
        this.loadDataAsync(() -> this.rankService.getAllRanks(), ranks -> {
            Objects.requireNonNull(this.grid).setItems(ranks);
        });
    }

    @Override
    protected void executeSearch() {
        final String query = Objects.requireNonNull(this.searchField).getValue();
        this.executeAsyncSearch(query, q -> this.rankService.searchRanks(q.trim()));
    }

    @Override
    protected HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);
        layout.add(new CreateButton(ignore -> Objects.requireNonNull(this.rankDialog).open(null)),
                new RefreshButton(ignore -> this.setupLazyDataProvider()));
        return layout;
    }

    @Override
    protected void configureGrid(final Grid<UserRankResponseDto> g) {
        g.addColumn(UserRankResponseDto::getId).setHeader("ID").setWidth("80px").setFlexGrow(0);

        g.addComponentColumn(rank -> this.createClickableSpan(rank.getName(),
                ignore -> Objects.requireNonNull(this.rankDialog).open(rank))).setHeader("Name").setFlexGrow(2);

        // Admin view column (unique pattern: check/ban icons)
        g.addComponentColumn(rank -> {
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

        g.addComponentColumn(this::createActionButtons).setHeader("Actions").setWidth("150px").setFlexGrow(0);
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
        final var deleteButton = new DeleteButton(ignore -> this.handleDeletion(() -> {
            this.rankService.deleteRank(Objects.requireNonNull(rank.getId()));
        }));

        layout.add(editButton, deleteButton);
        return layout;
    }
}
