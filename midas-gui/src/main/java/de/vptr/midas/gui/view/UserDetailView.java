package de.vptr.midas.gui.view;

import java.time.format.DateTimeFormatter;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.dto.UserGroupResponseDto;
import de.vptr.midas.gui.dto.UserResponseDto;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.UserGroupService;
import de.vptr.midas.gui.service.UserService;
import de.vptr.midas.gui.util.NotificationUtil;
import jakarta.inject.Inject;

/** Detail view for a single user. */
@Route(value = "users", layout = MainLayout.class)
public class UserDetailView extends AbstractAsyncDataView implements BeforeEnterObserver, HasUrlParameter<String> {

    private static final Logger LOG = Logger.getLogger(UserDetailView.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    @Inject
    UserService userService;

    @Inject
    UserGroupService userGroupService;

    @Inject
    AuthService authService;

    private String identifier;
    private Grid<UserGroupResponseDto> groupsGrid;

    /** Initializes UserDetailView. */
    public UserDetailView() {
        this.setSizeFull();
        this.setPadding(true);
        this.setSpacing(true);
    }

    @Override
    public void setParameter(final BeforeEvent event, final String parameter) {
        this.identifier = parameter;
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (!this.authService.isAuthenticated()) {
            event.forwardTo(LoginView.class);
            return;
        }

        this.buildUi();
    }

    private void buildUi() {
        this.removeAll();

        final var backButton = new Button("← Back to Users",
                _ -> this.getUI().ifPresent(ui -> ui.navigate(UserView.class)));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        this.add(backButton);

        this.loadUserAsync();
    }

    private void loadUserAsync() {
        if (this.identifier == null || this.identifier.trim().isEmpty()) {
            NotificationUtil.showError("No user identifier provided");
            return;
        }

        LOG.infof("Loading user with identifier: %s", this.identifier);

        final Long userId = this.tryParseId(this.identifier);

        if (userId != null) {
            this.loadUserById(userId);
        } else {
            this.loadUserByUsername(this.identifier);
        }
    }

    private Long tryParseId(final String value) {
        try {
            return Long.valueOf(value);
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    private void loadUserById(final Long userId) {
        this.loadDataAsync(() -> this.userService.getUserById(userId), optionalUser -> {
            if (optionalUser.isPresent()) {
                final var user = optionalUser.get();
                LOG.infof("Successfully loaded user: %s", user.getUsername());
                this.renderUser(user);
            } else {
                LOG.warnf("User not found with ID: %s", userId);
                this.renderNotFound();
            }
        });
    }

    private void loadUserByUsername(final String username) {
        this.loadDataAsync(() -> this.userService.getUserByUsername(username), optionalUser -> {
            if (optionalUser.isPresent()) {
                final var user = optionalUser.get();
                LOG.infof("Successfully loaded user: %s", user.getUsername());
                this.renderUser(user);
            } else {
                LOG.warnf("User not found with username: %s", username);
                this.renderNotFound();
            }
        });
    }

    private void renderUser(final UserResponseDto user) {
        final var titleHeader = new H2(user.getUsername());

        final var createdDate = user.getCreated();
        final var memberSince = new Span("Member since: "
                + (createdDate != null ? createdDate.format(DATE_FORMATTER) : "Unknown"));
        memberSince.getStyle().set("color", "var(--lumo-secondary-text-color)");

        // Account activity summary
        final var activityHeader = new H3("Activity");

        final var postsCount = new Span("Posts: "
                + (user.getPostsCount() != null ? user.getPostsCount() : 0));
        final var commentsCount = new Span("Comments: "
                + (user.getCommentsCount() != null ? user.getCommentsCount() : 0));

        final var statsLayout = new HorizontalLayout(postsCount, commentsCount);
        statsLayout.setSpacing(true);

        // Groups section
        final var groupsHeader = new H3("Groups");
        this.createGroupsGrid();

        this.add(titleHeader, memberSince, activityHeader, statsLayout, groupsHeader, this.groupsGrid);

        this.loadGroupsAsync(user.getId());
    }

    private void createGroupsGrid() {
        this.groupsGrid = new Grid<>(UserGroupResponseDto.class, false);
        this.groupsGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.groupsGrid.setPageSize(20);
        this.groupsGrid.setHeight("400px");

        this.groupsGrid.addColumn(UserGroupResponseDto::getName)
                .setHeader("Group Name")
                .setFlexGrow(1);
    }

    private void loadGroupsAsync(final Long userId) {
        this.loadDataAsync(() -> {
            LOG.infof("Loading groups for user: %s", userId);
            return this.userGroupService.getGroupsByUser(userId);
        }, groups -> {
            LOG.infof("Successfully loaded %s groups for user", groups.size());
            this.groupsGrid.setItems(groups);
        });
    }

    private void renderNotFound() {
        final var notFoundHeader = new H2("User Not Found");
        final var message = new Div();
        message.setText("The requested user could not be found.");
        message.getStyle().set("color", "var(--lumo-secondary-text-color)");

        this.add(notFoundHeader, message);
    }
}
