package de.vptr.midas.gui.view;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;

import de.vptr.midas.gui.dto.UserDto;
import de.vptr.midas.gui.dto.UserGroupResponseDto;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.UserGroupService;
import de.vptr.midas.gui.util.MidasErrorHandler;
import de.vptr.midas.gui.util.NotificationUtil;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** Detail view for a single user group. */
@Route(value = "groups/:identifier", layout = MainLayout.class)
public class UserGroupDetailView extends AbstractAsyncDataView implements BeforeEnterObserver {

    private static final Logger LOG = Logger.getLogger(UserGroupDetailView.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Inject
    UserGroupService groupService;

    @Inject
    AuthService authService;

    @Nullable
    private Grid<UserDto> membersGrid;
    @Nullable
    private H2 groupTitle;

    @Nullable
    private Long groupId;
    @Nullable
    private UserGroupResponseDto currentGroup;

    /** Initializes UserGroupDetailView. */
    public UserGroupDetailView() {
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

        final RouteParameters parameters = event.getRouteParameters();
        final String identifier = parameters.get("identifier").orElse(null);

        if (identifier == null || identifier.isEmpty()) {
            NotificationUtil.showError("Invalid group identifier");
            event.getUI().navigate(UserGroupView.class);
            return;
        }

        this.resolveGroupAndLoad(identifier);
    }

    private void resolveGroupAndLoad(final String identifier) {
        this.loadDataAsync(() -> {
            LOG.infof("Resolving group with identifier: %s", identifier);
            try {
                final Long id = Long.parseLong(identifier);
                return this.groupService.getGroupById(id).orElse(null);
            } catch (final NumberFormatException e) {
                LOG.infof("Identifier is not numeric, searching by name: %s", identifier);
                final var results = this.groupService.searchGroups(identifier);
                return results.stream().filter(group -> identifier.equalsIgnoreCase(group.getName())).findFirst()
                        .orElse(null);
            }
        }, group -> {
            if (group == null) {
                NotificationUtil.showError("Group not found");
                this.getUI().ifPresent(ui -> ui.navigate(UserGroupView.class));
                return;
            }
            this.currentGroup = group;
            this.groupId = group.getId();
            this.buildUi();
            this.loadMembersAsync();
        }, MidasErrorHandler::handleInUiContext, 30);
    }

    private void buildUi() {
        this.removeAll();

        final var backButton = new Button("Back to Groups", new Icon(VaadinIcon.ARROW_LEFT), e -> {
            this.getUI().ifPresent(ui -> ui.navigate(UserGroupView.class));
        });
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        final var headerLayout = new HorizontalLayout();
        headerLayout.setAlignItems(Alignment.CENTER);
        headerLayout.setSpacing(true);
        headerLayout.add(backButton);

        this.groupTitle = new H2(Objects.requireNonNull(this.currentGroup).getName());

        this.createMembersGrid();

        this.add(headerLayout, this.groupTitle, Objects.requireNonNull(this.membersGrid));
    }

    private void createMembersGrid() {
        this.membersGrid = new Grid<>(UserDto.class, false);
        this.membersGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.membersGrid.setSizeFull();

        this.membersGrid.addColumn(UserDto::getUsername).setHeader("Username").setFlexGrow(2).setSortable(true);

        this.membersGrid.addColumn(user -> {
            final LocalDateTime created = user.getCreated();
            return created != null ? created.format(DATE_FORMATTER) : "";
        }).setHeader("Joined").setFlexGrow(1).setSortable(true);

        this.membersGrid.setPageSize(20);
    }

    private void loadMembersAsync() {
        final var id = Objects.requireNonNull(this.groupId);
        LOG.infof("Starting async member loading for group: %s", id);

        this.loadDataAsync(() -> {
            LOG.info("Making REST call to load group members");
            return this.groupService.getUsersInGroup(id);
        }, members -> {
            LOG.infof("Successfully loaded %s members", members.size());
            Objects.requireNonNull(this.membersGrid).setItems(members);
        }, MidasErrorHandler::handleInUiContext, 30);
    }
}
