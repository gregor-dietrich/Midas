package de.vptr.midas.gui.view;

import java.util.Objects;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLayout;

import de.vptr.midas.gui.component.button.LogoutButton;
import de.vptr.midas.gui.component.button.ThemeToggleButton;
import de.vptr.midas.gui.component.button.UserViewButton;
import de.vptr.midas.gui.component.tabs.AdminNavigationTabs;
import de.vptr.midas.gui.dto.UserRankResponseDto;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.ThemeService;
import de.vptr.midas.gui.service.UserRankService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** Layout for admin main. */
public class AdminMainLayout extends VerticalLayout implements RouterLayout, BeforeEnterObserver {

    private static final Logger LOG = Logger.getLogger(AdminMainLayout.class);
    @Nullable
    private Button userViewButton;
    @Nullable
    private Button logoutButton;
    @Nullable
    private Tabs navigationTabs;
    @Nullable
    private transient UserRankResponseDto cachedUserRank = null;
    private boolean cachedUserRankValid = false;

    @Inject
    transient AuthService authService;

    @Inject
    transient ThemeService themeService;

    @Inject
    transient UserRankService userRankService;

    @Nullable
    private HorizontalLayout topBar;
    @Nullable
    private HorizontalLayout rightSide;
    @Nullable
    private HorizontalLayout mainLayout;
    @Nullable
    private VerticalLayout sidebar;
    @Nullable
    private VerticalLayout contentArea;
    private boolean initialized = false;

    /**
     * Get the shared top bar for views that need to add additional components
     */
    @Nullable
    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Expose top bar intentionally so child views can add components")
    public HorizontalLayout getTopBar() {
        return this.topBar;
    }

    @Override
    public void showRouterLayoutContent(final HasElement content) {
        if (this.contentArea != null) {
            this.contentArea.getElement().removeAllChildren();
            this.contentArea.getElement().appendChild(content.getElement());
        } else {
            // Fallback to default behavior if content area is not initialized
            this.getElement().appendChild(content.getElement());
        }
    }

    @Override
    protected void onAttach(final AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.updateLogoutButtonVisibility();
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (!this.initialized) {
            this.initializeLayout();
            this.initialized = true;
        }

        // Apply current theme on every navigation
        this.themeService.applyTheme(this.themeService.getCurrentTheme());

        final var targetView = event.getNavigationTarget();

        LOG.tracef("AdminMainLayout.beforeEnter - Target: %s", targetView.getSimpleName());

        // Skip all checks for the backend error view
        if (targetView == BackendErrorView.class) {
            LOG.trace("Navigating to backend error view, skipping all checks");
            this.removeButtonsFromTopBar(); // Hide logout button on error view
            this.hideNavigationTabs(); // Hide navigation tabs on error view
            return;
        }

        // Skip auth check for the login view
        if (targetView == LoginView.class) {
            LOG.trace("Navigating to login view, skipping authentication check");
            this.removeButtonsFromTopBar(); // Hide logout button on login view
            this.hideNavigationTabs(); // Hide navigation tabs on login view
            return;
        }

        // Check authentication for all other views
        if (!this.authService.isAuthenticated()) {
            LOG.trace("User not authenticated, redirecting to login");
            event.forwardTo(LoginView.class);
            return;
        }

        // Check admin permission for admin views (use cached rank to avoid duplicate calls)
        if (!this.getCachedUserRank().canAdminView()) {
            LOG.trace("User does not have admin:view permission, redirecting to home");
            event.forwardTo(HomeView.class);
            return;
        }

        // User is authenticated and has admin permission - show logout button and
        // navigation tabs
        this.addButtonsToTopBar();
        this.showNavigationTabs();

        LOG.tracef("All checks passed for %s", targetView.getSimpleName());
    }

    private void updateLogoutButtonVisibility() {
        if (this.authService.isAuthenticated()) {
            // Check current route to determine if logout button should be shown
            this.getUI().ifPresent(ui -> {
                final var location = ui.getInternals().getActiveViewLocation();
                if (location != null) {
                    final var path = location.getPath();
                    // Don't show logout button on login or error views
                    if (!"login".equals(path) && !"backend-error".equals(path)) {
                        this.addButtonsToTopBar();
                        this.showNavigationTabs();
                    } else {
                        this.removeButtonsFromTopBar();
                        this.hideNavigationTabs();
                    }
                }
            });
        } else {
            this.removeButtonsFromTopBar();
            this.hideNavigationTabs();
        }
    }

    private void initializeLayout() {
        this.setSizeFull();
        this.setPadding(false);
        this.setSpacing(false);

        this.createTopBar();
        this.createMainLayout();
    }

    private void createTopBar() {
        this.topBar = new HorizontalLayout();
        this.topBar.setWidthFull();
        this.topBar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        this.topBar.setPadding(true);

        // Create left side with title
        final var leftSide = new HorizontalLayout();
        final var title = new H1("Midas Admin Panel");
        title.getStyle().set("margin", "0");
        title.getStyle().set("color", "var(--lumo-primary-text-color)");
        leftSide.add(title);

        // Create right side components container
        this.rightSide = new HorizontalLayout();
        this.rightSide.setSpacing(true);

        final var themeToggle = new ThemeToggleButton(this.themeService);
        this.rightSide.add(themeToggle);

        this.topBar.add(leftSide, this.rightSide);
        this.addComponentAsFirst(this.topBar);
    }

    private void createMainLayout() {
        // Create the main horizontal layout that will contain sidebar and content
        this.mainLayout = new HorizontalLayout();
        this.mainLayout.setSizeFull();
        this.mainLayout.setPadding(false);
        this.mainLayout.setSpacing(false);

        // Create sidebar
        this.sidebar = new VerticalLayout();
        this.sidebar.setWidth("250px");
        this.sidebar.setHeightFull();
        this.sidebar.setPadding(true);
        this.sidebar.setSpacing(false);
        this.sidebar.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        this.sidebar.getStyle().set("border-right", "1px solid var(--lumo-contrast-10pct)");

        // Create navigation tabs (use cached rank)
        this.navigationTabs = new AdminNavigationTabs(this.getCachedUserRank());
        Objects.requireNonNull(this.sidebar).add(this.navigationTabs);

        // Create content area where the actual views will be displayed
        this.contentArea = new VerticalLayout();
        this.contentArea.setSizeFull();
        this.contentArea.setPadding(false);
        this.contentArea.setSpacing(false);

        // Add sidebar and content area to main layout
        this.mainLayout.add(this.sidebar, this.contentArea);
        this.mainLayout.setFlexGrow(0, this.sidebar);
        this.mainLayout.setFlexGrow(1, this.contentArea);

        this.add(this.mainLayout);
    }

    private void showNavigationTabs() {
        if (this.sidebar != null && this.mainLayout != null) {
            this.sidebar.setVisible(true);
        }
    }

    private void hideNavigationTabs() {
        if (this.sidebar != null) {
            this.sidebar.setVisible(false);
        }
    }

    private void addButtonsToTopBar() {
        if (this.rightSide != null) {
            // Remove existing buttons if present
            if (this.logoutButton != null) {
                this.rightSide.remove(this.logoutButton);
            }
            if (this.userViewButton != null) {
                this.rightSide.remove(this.userViewButton);
            }

            // Create new logout button
            this.logoutButton = new LogoutButton(ignore -> {
                this.authService.logout();
                // Clear cached user rank on logout
                this.userRankService.clearCachedUserRank();
                this.cachedUserRankValid = false; // Clear request-scope cache
                this.getUI().ifPresent(ui -> ui.navigate(LoginView.class));
            });

            // Create new AdminView button
            this.userViewButton =
                    new UserViewButton(ignore -> this.getUI().ifPresent(ui -> ui.navigate(HomeView.class)));

            // Add buttons
            final var componentCount = this.rightSide.getComponentCount();
            if (componentCount > 0) {
                this.rightSide.addComponentAtIndex(componentCount - 1, this.logoutButton);
                this.rightSide.addComponentAtIndex(componentCount - 1, this.userViewButton);
            } else {
                this.rightSide.add(this.logoutButton);
                this.rightSide.add(this.userViewButton);
            }
        }
    }

    private void removeButtonsFromTopBar() {
        if (this.rightSide != null) {
            if (this.logoutButton != null) {
                this.rightSide.remove(this.logoutButton);
                this.logoutButton = null;
            }
            if (this.userViewButton != null) {
                this.rightSide.remove(this.userViewButton);
                this.userViewButton = null;
            }
        }
    }

    /**
     * Returns the current user rank from service, caching it for the duration of this layout instance. This avoids
     * redundant service calls during the same navigation/render cycle.
     */
    private UserRankResponseDto getCachedUserRank() {
        if (!this.cachedUserRankValid) {
            this.cachedUserRank = this.userRankService.getCurrentUserRank();
            this.cachedUserRankValid = true;
        }
        return Objects.requireNonNull(this.cachedUserRank);
    }
}
