package de.vptr.midas.gui.view;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.RouterLayout;

import de.vptr.midas.gui.component.button.AdminViewButton;
import de.vptr.midas.gui.component.button.LogoutButton;
import de.vptr.midas.gui.component.button.ThemeToggleButton;
import de.vptr.midas.gui.component.tabs.NavigationTabs;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.BackendAvailabilityService;
import de.vptr.midas.gui.service.ThemeService;
import de.vptr.midas.gui.service.UserRankService;
import jakarta.inject.Inject;

/** Layout for main. */
public class MainLayout extends VerticalLayout implements RouterLayout, BeforeEnterObserver {

    private static final Logger LOG = Logger.getLogger(MainLayout.class);
    private Button adminViewButton;
    private Button logoutButton;
    private Tabs navigationTabs;
    private boolean cachedAdminPermission = false;
    private boolean cachedAdminPermissionValid = false;

    @Inject
    AuthService authService;

    @Inject
    BackendAvailabilityService backendAvailabilityService;

    @Inject
    ThemeService themeService;

    @Inject
    UserRankService userRankService;

    private HorizontalLayout topBar;
    private HorizontalLayout rightSide;
    private boolean initialized = false;

    /**
     * Get the shared top bar for views that need to add additional components
     */
    public HorizontalLayout getTopBar() {
        return this.topBar;
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

        LOG.tracef("MainLayout.beforeEnter - Target: %s", targetView.getSimpleName());

        // Skip all checks for the backend error view
        if (targetView == BackendErrorView.class) {
            LOG.trace("Navigating to backend error view, skipping all checks");
            this.removeButtonsFromTopBar(); // Hide logout button on error view
            this.hideNavigationTabs(); // Hide navigation tabs on error view
            return;
        }

        // Check backend availability (fault tolerance handles retry/timeout/fallback)
        try {
            if (!this.backendAvailabilityService.isBackendAvailable()) {
                LOG.error("Backend unavailable, redirecting to error page");
                event.forwardTo(BackendErrorView.class);
                return;
            }
        } catch (final RuntimeException e) {
            LOG.error("Backend health check failed", e);
            event.forwardTo(BackendErrorView.class);
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

        // User is authenticated - show logout button and navigation tabs
        this.addButtonsToTopBar();
        this.showNavigationTabs();

        LOG.tracef("All checks passed for %s", targetView.getSimpleName());
    }

    private void updateLogoutButtonVisibility() {
        if (!this.authService.isAuthenticated()) {
            this.removeButtonsFromTopBar();
            this.hideNavigationTabs();
            return;
        }

        // Check current route to determine if logout button should be shown
        this.getUI().ifPresent(ui -> {
            final var location = ui.getInternals().getActiveViewLocation();
            if (location == null) {
                return;
            }

            final var path = location.getPath();
            // Don't show logout button on login or error views
            if ("login".equals(path) || "backend-error".equals(path)) {
                this.removeButtonsFromTopBar();
                this.hideNavigationTabs();
                return;
            }

            // Don't add buttons if this is an admin route (AdminMainLayout will handle it)
            if (!path.startsWith("admin")) {
                this.addButtonsToTopBar();
                this.showNavigationTabs();
            }
        });
    }

    private void initializeLayout() {
        this.setSizeFull();
        this.setPadding(false);
        this.setSpacing(false);

        this.createTopBar();
    }

    private void createTopBar() {
        this.topBar = new HorizontalLayout();
        this.topBar.setWidthFull();
        this.topBar.setJustifyContentMode(JustifyContentMode.BETWEEN);
        this.topBar.setPadding(true);

        // Create navigation menu
        this.navigationTabs = new NavigationTabs();

        // Create right side components container
        this.rightSide = new HorizontalLayout();
        this.rightSide.setSpacing(true);

        final var themeToggle = new ThemeToggleButton(this.themeService);
        this.rightSide.add(themeToggle);

        this.topBar.add(this.rightSide);
        this.addComponentAsFirst(this.topBar);
    }

    private void showNavigationTabs() {
        if (this.navigationTabs == null || this.topBar == null
                || this.topBar.getChildren().anyMatch(component -> component == this.navigationTabs)) {
            return;
        }

        this.topBar.removeAll();
        this.topBar.add(this.navigationTabs, this.rightSide);
        this.topBar.setJustifyContentMode(JustifyContentMode.BETWEEN);
    }

    private void hideNavigationTabs() {
        if (this.navigationTabs == null || this.topBar == null) {
            return;
        }

        this.topBar.removeAll();
        this.topBar.add(this.rightSide);
        this.topBar.setJustifyContentMode(JustifyContentMode.END);
    }

    private void addButtonsToTopBar() {
        if (this.rightSide == null) {
            return;
        }

        // Remove existing buttons if present
        if (this.logoutButton != null) {
            this.rightSide.remove(this.logoutButton);
        }
        if (this.adminViewButton != null) {
            this.rightSide.remove(this.adminViewButton);
        }

        // Create new logout button
        this.logoutButton = new LogoutButton(_ -> {
            this.authService.logout();
            // Clear cached user rank on logout
            this.userRankService.clearCachedUserRank();
            this.cachedAdminPermissionValid = false; // Clear request-scope cache
            this.getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        });

        // Only create admin button if user has admin:view permission (cached per layout instance)
        if (!this.cachedAdminPermissionValid) {
            this.cachedAdminPermission = this.userRankService.getCurrentUserRank().canAdminView();
            this.cachedAdminPermissionValid = true;
        }
        if (this.cachedAdminPermission) {
            this.adminViewButton = new AdminViewButton(
                    _ -> this.getUI().ifPresent(ui -> ui.navigate(AdminHomeView.class)));
        }

        // Add buttons
        final var componentCount = this.rightSide.getComponentCount();
        if (componentCount > 0) {
            this.rightSide.addComponentAtIndex(componentCount - 1, this.logoutButton);
            if (this.adminViewButton != null) {
                this.rightSide.addComponentAtIndex(componentCount - 1, this.adminViewButton);
            }
            return;
        }

        if (this.adminViewButton != null) {
            this.rightSide.add(this.adminViewButton);
        }
        this.rightSide.add(this.logoutButton);
    }

    private void removeButtonsFromTopBar() {
        if (this.rightSide == null) {
            return;
        }

        if (this.logoutButton != null) {
            this.rightSide.remove(this.logoutButton);
            this.logoutButton = null;
        }
        if (this.adminViewButton != null) {
            this.rightSide.remove(this.adminViewButton);
            this.adminViewButton = null;
        }
    }
}
