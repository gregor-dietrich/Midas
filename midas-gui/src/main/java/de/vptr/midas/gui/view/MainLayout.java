package de.vptr.midas.gui.view;

import java.util.Objects;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;

import de.vptr.midas.gui.component.button.AdminViewButton;
import de.vptr.midas.gui.component.button.ThemeToggleButton;
import de.vptr.midas.gui.component.tabs.NavigationTabs;
import jakarta.annotation.Nullable;

/** Layout for main. */
public class MainLayout extends AbstractMainLayoutBase {

    private boolean cachedAdminPermission = false;
    private boolean cachedAdminPermissionValid = false;

    @Override
    protected boolean checkSpecificPermissions(final BeforeEnterEvent event) {
        return true;
    }

    @Override
    protected void initializeLayout() {
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

        this.navigationTabs = new NavigationTabs();

        this.rightSide = new HorizontalLayout();
        this.rightSide.setSpacing(true);
        this.rightSide.add(new ThemeToggleButton(this.themeService));

        this.topBar.add(this.rightSide);
        this.addComponentAsFirst(this.topBar);
    }

    @Override
    // Identity is the question being asked: whether this exact component instance is
    // already attached to the top bar. Vaadin components do not define value equality.
    @SuppressWarnings("ReferenceEquality")
    protected void showNavigationTabs() {
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

    @Override
    protected void hideNavigationTabs() {
        if (this.navigationTabs == null || this.topBar == null) {
            return;
        }

        final var tb = Objects.requireNonNull(this.topBar);
        final var rs = Objects.requireNonNull(this.rightSide);
        tb.removeAll();
        tb.add(rs);
        tb.setJustifyContentMode(JustifyContentMode.END);
    }

    @Override
    protected void updateLogoutButtonVisibility() {
        if (!this.authService.isAuthenticated()) {
            this.removeButtonsFromTopBar();
            this.hideNavigationTabs();
            return;
        }

        this.getUI().ifPresent(ui -> {
            final var location = ui.getInternals().getActiveViewLocation();
            if (location == null) {
                return;
            }

            final var path = location.getPath();
            if ("login".equals(path) || "backend-error".equals(path)) {
                this.removeButtonsFromTopBar();
                this.hideNavigationTabs();
                return;
            }

            if (!path.startsWith("admin")) {
                this.addButtonsToTopBar();
                this.showNavigationTabs();
            }
        });
    }

    @Nullable
    @Override
    protected Button createExtraNavigationButton() {
        if (!this.cachedAdminPermissionValid) {
            this.cachedAdminPermission = this.userRankService.getCurrentUserRank().canAdminView();
            this.cachedAdminPermissionValid = true;
        }
        if (this.cachedAdminPermission) {
            return new AdminViewButton(ignore -> this.getUI().ifPresent(ui -> ui.navigate(AdminHomeView.class)));
        }
        return null;
    }

    @Override
    protected void onLogout() {
        this.cachedAdminPermissionValid = false;
    }
}
