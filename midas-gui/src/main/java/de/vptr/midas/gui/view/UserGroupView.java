package de.vptr.midas.gui.view;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.component.layout.AsyncSearchLayout;
import de.vptr.midas.gui.dto.UserGroupResponseDto;
import de.vptr.midas.gui.dto.UserResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.UserGroupService;
import de.vptr.midas.gui.service.UserService;
import de.vptr.midas.gui.util.MidasErrorHandler;
import de.vptr.midas.gui.util.UiConstants;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** View for browsing and searching user groups. */
@Route(value = "groups", layout = MainLayout.class)
public class UserGroupView extends AbstractAsyncDataView implements BeforeEnterObserver {

    private static final Logger LOG = Logger.getLogger(UserGroupView.class);

    @Inject
    UserGroupService groupService;

    @Inject
    AuthService authService;

    @Inject
    UserService userService;

    @Nullable
    private Grid<UserGroupResponseDto> myGroupsGrid;
    @Nullable
    private Grid<UserGroupResponseDto> otherGroupsGrid;
    @Nullable
    private AsyncSearchLayout searchLayout;

    @Nullable
    private Long currentUserId;

    /** Initializes UserGroupView. */
    public UserGroupView() {
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
        this.loadGroupsAsync();
    }

    private void buildUi() {
        this.removeAll();

        final var title = new H2("Groups");
        this.searchLayout =
                new AsyncSearchLayout(this::searchGroups, this::loadGroupsAsync, "Search by name...", "Search Groups");

        this.createMyGroupsGrid();
        this.createOtherGroupsGrid();

        final var myGroupsHeader = new H3("My Groups");
        final var otherGroupsHeader = new H3("Other Groups");

        this.add(title, this.searchLayout, myGroupsHeader, Objects.requireNonNull(this.myGroupsGrid), otherGroupsHeader,
                Objects.requireNonNull(this.otherGroupsGrid));
    }

    private void createMyGroupsGrid() {
        this.myGroupsGrid = new Grid<>(UserGroupResponseDto.class, false);
        this.myGroupsGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.myGroupsGrid.setWidthFull();
        this.myGroupsGrid.setHeight("400px");

        this.myGroupsGrid.addComponentColumn(group -> {
            final var nameSpan = new Span(group.getName());
            nameSpan.getStyle().set("color", "var(--lumo-primary-text-color)");
            nameSpan.getStyle().set("cursor", "pointer");
            nameSpan.getStyle().set("width", "100%");
            nameSpan.getStyle().set("display", "block");
            nameSpan.addClickListener(e -> this.navigateToGroup(group));
            return nameSpan;
        }).setHeader("Name").setFlexGrow(2).setSortable(true);

        this.myGroupsGrid.addColumn(group -> group.getUserCount() != null ? group.getUserCount() : 0)
                .setHeader("Members").setWidth("120px").setFlexGrow(0).setSortable(true);

        this.myGroupsGrid.setPageSize(20);
    }

    private void createOtherGroupsGrid() {
        this.otherGroupsGrid = new Grid<>(UserGroupResponseDto.class, false);
        this.otherGroupsGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.otherGroupsGrid.setSizeFull();

        this.otherGroupsGrid.addComponentColumn(group -> {
            final var nameSpan = new Span(group.getName());
            nameSpan.getStyle().set("color", "var(--lumo-primary-text-color)");
            nameSpan.getStyle().set("cursor", "pointer");
            nameSpan.getStyle().set("width", "100%");
            nameSpan.getStyle().set("display", "block");
            nameSpan.addClickListener(e -> this.navigateToGroup(group));
            return nameSpan;
        }).setHeader("Name").setFlexGrow(2).setSortable(true);

        this.otherGroupsGrid.addColumn(group -> group.getUserCount() != null ? group.getUserCount() : 0)
                .setHeader("Members").setWidth("120px").setFlexGrow(0).setSortable(true);

        this.otherGroupsGrid.setPageSize(20);
    }

    private void navigateToGroup(final UserGroupResponseDto group) {
        this.getUI().ifPresent(ui -> ui.navigate("groups/" + group.getId()));
    }

    private void loadGroupsAsync() {
        LOG.info("Starting async group loading");

        this.loadDataAsync(() -> {
            LOG.info("Making REST calls to load groups and current user");
            final var currentUser = this.userService.getCurrentUser().orElse(null);
            final var allGroups = this.groupService.getAllGroups(0, UiConstants.DEFAULT_PAGE_SIZE);

            List<UserGroupResponseDto> myGroups = Collections.emptyList();
            if (currentUser != null) {
                myGroups = this.groupService.getGroupsByUser(Objects.requireNonNull(currentUser.getId()));
            }

            return new GroupLoadResult(allGroups, myGroups, currentUser);
        }, result -> {
            LOG.infof("Successfully loaded %s total groups, %s user groups", result.allGroups.size(),
                    result.myGroups.size());

            if (result.currentUser != null) {
                this.currentUserId = result.currentUser.getId();
            }

            this.updateGrids(result.allGroups, result.myGroups);
        });
    }

    private void updateGrids(final List<UserGroupResponseDto> allGroups, final List<UserGroupResponseDto> myGroups) {
        final Set<Long> myGroupIds = myGroups.stream().map(UserGroupResponseDto::getId).collect(Collectors.toSet());

        final var otherGroups = allGroups.stream().filter(group -> !myGroupIds.contains(group.getId())).toList();

        Objects.requireNonNull(this.myGroupsGrid).setItems(myGroups);
        Objects.requireNonNull(this.otherGroupsGrid).setItems(otherGroups);
    }

    private void searchGroups(final String query) {
        final var sl = Objects.requireNonNull(this.searchLayout);
        sl.setSearching(true);
        LOG.infof("Starting async group search with query: %s", query);

        final Long capturedUserId = this.currentUserId;

        this.loadDataAsync(() -> {
            LOG.info("Making REST call to search groups");
            final var searchResults = this.groupService.searchGroups(query);

            List<UserGroupResponseDto> myGroups = Collections.emptyList();
            if (capturedUserId != null) {
                myGroups = this.groupService.getGroupsByUser(capturedUserId);
            }

            return new GroupLoadResult(searchResults, myGroups, null);
        }, result -> {
            sl.setSearching(false);
            LOG.infof("Successfully found %s groups matching query", result.allGroups.size());

            final Set<Long> myGroupIds =
                    result.myGroups.stream().map(UserGroupResponseDto::getId).collect(Collectors.toSet());

            final var myResults =
                    result.allGroups.stream().filter(group -> myGroupIds.contains(group.getId())).toList();

            final var otherResults =
                    result.allGroups.stream().filter(group -> !myGroupIds.contains(group.getId())).toList();

            Objects.requireNonNull(this.myGroupsGrid).setItems(myResults);
            Objects.requireNonNull(this.otherGroupsGrid).setItems(otherResults);
        }, throwable -> {
            sl.setSearching(false);
            MidasErrorHandler.handleInUiContext(throwable);
        });
    }

    private static class GroupLoadResult {
        final List<UserGroupResponseDto> allGroups;
        final List<UserGroupResponseDto> myGroups;
        @Nullable
        final UserResponseDto currentUser;

        GroupLoadResult(final List<UserGroupResponseDto> allGroups, final List<UserGroupResponseDto> myGroups,
                @Nullable final UserResponseDto currentUser) {
            this.allGroups = allGroups;
            this.myGroups = myGroups;
            this.currentUser = currentUser;
        }
    }
}
