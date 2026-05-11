package de.vptr.midas.gui.view;

import java.util.Objects;

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
import de.vptr.midas.gui.service.ThemeService;
import de.vptr.midas.gui.service.UserRankService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** Layout for main. */
public class MainLayout extends VerticalLayout implements RouterLayout, BeforeEnterObserver {

    private static final Logger LOG = Logger.getLogger(MainLayout.class);
    @Nullable
    private Button adminViewButton;
    @Nullable
    private Button logoutButton;
    @Nullable
    private Tabs navigationTabs;
    private boolean cachedAdminPermission = false;
    private boolean cachedAdminPermissionValid = false;

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
    protected void onAttach(final AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.updateLogoutButtonVisibility();
    }

    // CPD-OFF
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
    // CPD-ON

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

        final var tb = Objects.requireNonNull(this.topBar);
        final var rs = Objects.requireNonNull(this.rightSide);
        tb.removeAll();
        tb.add(this.navigationTabs, rs);
        tb.setJustifyContentMode(JustifyContentMode.BETWEEN);
    }

    private void hideNavigationTabs() {
        if (this.navigationTabs == null || this.topBar == null) {
            return;
        }

        final var tb = Objects.requireNonNull(this.topBar);
        final var rs = Objects.requireNonNull(this.rightSide);
        tb.removeAll();
        tb.add(rs);
        tb.setJustifyContentMode(JustifyContentMode.END);
    }

    private void addButtonsToTopBar() {
        if (this.rightSide == null) {
            return;
        }
        final var rs = Objects.requireNonNull(this.rightSide);

        // Remove existing buttons if present
        if (this.logoutButton != null) {
            rs.remove(this.logoutButton);
        }
        if (this.adminViewButton != null) {
            rs.remove(this.adminViewButton);
        }

        // Create new logout button
        this.logoutButton = new LogoutButton(e -> {
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
            this.adminViewButton =
                    new AdminViewButton(e -> this.getUI().ifPresent(ui -> ui.navigate(AdminHomeView.class)));
        }

        // Add buttons
        final var componentCount = rs.getComponentCount();
        if (componentCount > 0) {
            rs.addComponentAtIndex(componentCount - 1, Objects.requireNonNull(this.logoutButton));
            if (this.adminViewButton != null) {
                rs.addComponentAtIndex(componentCount - 1, this.adminViewButton);
            }
            return;
        }

        if (this.adminViewButton != null) {
            rs.add(this.adminViewButton);
        }
        rs.add(Objects.requireNonNull(this.logoutButton));
    }

    private void removeButtonsFromTopBar() {
        if (this.rightSide == null) {
            return;
        }
        final var rs = Objects.requireNonNull(this.rightSide);

        if (this.logoutButton != null) {
            rs.remove(this.logoutButton);
            this.logoutButton = null;
        }
        if (this.adminViewButton != null) {
            rs.remove(this.adminViewButton);
            this.adminViewButton = null;
        }
    }
}
