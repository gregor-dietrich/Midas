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

import de.vptr.midas.gui.component.button.LogoutButton;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.ThemeService;
import de.vptr.midas.gui.service.UserRankService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** Base class for main layouts. */
public abstract class AbstractMainLayoutBase extends VerticalLayout implements RouterLayout, BeforeEnterObserver {

    private final Logger log = Logger.getLogger(this.getClass());

    @Inject
    protected transient AuthService authService;

    @Inject
    protected transient ThemeService themeService;

    @Inject
    protected transient UserRankService userRankService;

    @Nullable
    protected Button extraNavigationButton;
    @Nullable
    protected Button logoutButton;
    @Nullable
    protected Tabs navigationTabs;

    @Nullable
    protected HorizontalLayout topBar;
    @Nullable
    protected HorizontalLayout rightSide;
    protected boolean initialized = false;

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

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (!this.initialized) {
            this.initializeLayout();
            this.initialized = true;
        }

        this.themeService.applyTheme(this.themeService.getCurrentTheme());

        final var targetView = event.getNavigationTarget();
        this.log.tracef("Layout beforeEnter - Target: %s", targetView.getSimpleName());

        if (targetView == BackendErrorView.class || targetView == LoginView.class) {
            this.removeButtonsFromTopBar();
            this.hideNavigationTabs();
            return;
        }

        if (!this.authService.isAuthenticated()) {
            event.forwardTo(LoginView.class);
            return;
        }

        if (!this.checkSpecificPermissions(event)) {
            return;
        }

        this.addButtonsToTopBar();
        this.showNavigationTabs();
    }

    protected abstract void initializeLayout();

    protected abstract boolean checkSpecificPermissions(BeforeEnterEvent event);

    protected abstract void showNavigationTabs();

    protected abstract void hideNavigationTabs();

    protected abstract void updateLogoutButtonVisibility();

    protected void addButtonsToTopBar() {
        if (this.rightSide == null) {
            return;
        }
        final var rs = Objects.requireNonNull(this.rightSide);

        if (this.logoutButton != null) {
            rs.remove(this.logoutButton);
        }
        if (this.extraNavigationButton != null) {
            rs.remove(this.extraNavigationButton);
        }

        this.logoutButton = new LogoutButton(ignore -> {
            this.authService.logout();
            this.userRankService.clearCachedUserRank();
            this.onLogout();
            this.getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        });

        this.extraNavigationButton = this.createExtraNavigationButton();

        final var componentCount = rs.getComponentCount();
        if (componentCount > 0) {
            rs.addComponentAtIndex(componentCount - 1, Objects.requireNonNull(this.logoutButton));
            if (this.extraNavigationButton != null) {
                rs.addComponentAtIndex(componentCount - 1, this.extraNavigationButton);
            }
            return;
        }

        if (this.extraNavigationButton != null) {
            rs.add(this.extraNavigationButton);
        }
        rs.add(Objects.requireNonNull(this.logoutButton));
    }

    protected void removeButtonsFromTopBar() {
        if (this.rightSide == null) {
            return;
        }
        final var rs = Objects.requireNonNull(this.rightSide);

        if (this.logoutButton != null) {
            rs.remove(this.logoutButton);
            this.logoutButton = null;
        }
        if (this.extraNavigationButton != null) {
            rs.remove(this.extraNavigationButton);
            this.extraNavigationButton = null;
        }
    }

    @Nullable
    protected abstract Button createExtraNavigationButton();

    protected abstract void onLogout();
}
