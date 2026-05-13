package de.vptr.midas.gui.view;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.component.dialog.ChangePasswordDialog;
import de.vptr.midas.gui.dto.UserGroupResponseDto;
import de.vptr.midas.gui.dto.UserResponseDto;
import de.vptr.midas.gui.service.UserGroupService;
import de.vptr.midas.gui.service.UserService;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** Detail view for a single user. */
@Route(value = "users", layout = MainLayout.class)
public class UserDetailView extends AbstractDetailViewBase<UserResponseDto> {

    private static final Logger LOG = Logger.getLogger(UserDetailView.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @Inject
    transient UserService userService;

    @Inject
    transient UserGroupService userGroupService;

    @Nullable
    private Grid<UserGroupResponseDto> groupsGrid;

    @Override
    protected String getEntityPluralName() {
        return "Users";
    }

    @Override
    protected String getEntitySingleName() {
        return "User";
    }

    @Override
    protected Class<? extends Component> getBackNavigationTarget() {
        return UserView.class;
    }

    @Override
    protected void loadById(final Long id) {
        this.loadDataAsync(() -> this.userService.getUserById(id),
                optionalUser -> this.handleLoadResult(optionalUser, id));
    }

    @Override
    protected void loadByName(final String name) {
        this.loadDataAsync(() -> this.userService.getUserByUsername(name),
                optionalUser -> this.handleLoadResult(optionalUser, name));
    }

    @Override
    protected void renderEntity(final UserResponseDto user) {
        final var titleHeader = new H2(user.getUsername());

        final var createdDate = user.getCreated();
        final var memberSince =
                new Span("Member since: " + (createdDate != null ? createdDate.format(DATE_FORMATTER) : "Unknown"));
        memberSince.getStyle().set("color", "var(--lumo-secondary-text-color)");

        final var activityHeader = new H3("Activity");

        final var postsCount = new Span("Posts: " + (user.getPostsCount() != null ? user.getPostsCount() : 0));
        final var commentsCount =
                new Span("Comments: " + (user.getCommentsCount() != null ? user.getCommentsCount() : 0));

        final var statsLayout = new HorizontalLayout(postsCount, commentsCount);
        statsLayout.setSpacing(true);

        final var groupsHeader = new H3("Groups");
        this.createGroupsGrid();

        this.add(titleHeader, memberSince);

        if (this.isCurrentUser(user)) {
            final var changePasswordButton =
                    new Button("Change Password", ignore -> new ChangePasswordDialog(this.userService).open());
            changePasswordButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            this.add(changePasswordButton);
        }

        this.add(activityHeader, statsLayout, groupsHeader, Objects.requireNonNull(this.groupsGrid));

        this.loadGroupsAsync(Objects.requireNonNull(user.getId()));
    }

    private boolean isCurrentUser(final UserResponseDto user) {
        final var currentUsername = this.authService.getUsername();
        return currentUsername != null && currentUsername.equals(user.getUsername());
    }

    private void createGroupsGrid() {
        this.groupsGrid = new Grid<>(UserGroupResponseDto.class, false);
        this.groupsGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.groupsGrid.setPageSize(20);
        this.groupsGrid.setHeight("400px");

        this.groupsGrid.addColumn(UserGroupResponseDto::getName).setHeader("Group Name").setFlexGrow(1);
    }

    private void loadGroupsAsync(final Long userId) {
        this.loadDataAsync(() -> {
            LOG.infof("Loading groups for user: %s", userId);
            return this.userGroupService.getGroupsByUser(userId);
        }, groups -> {
            LOG.infof("Successfully loaded %s groups for user", groups.size());
            Objects.requireNonNull(this.groupsGrid).setItems(groups);
        });
    }
}
