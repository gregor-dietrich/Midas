package de.vptr.midas.gui.view;

import java.util.Objects;

import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;

import de.vptr.midas.gui.component.button.ThemeToggleButton;
import de.vptr.midas.gui.component.button.UserViewButton;
import de.vptr.midas.gui.component.tabs.AdminNavigationTabs;
import de.vptr.midas.gui.dto.UserRankResponseDto;
import jakarta.annotation.Nullable;

/** Layout for admin main. */
public class AdminMainLayout extends AbstractMainLayoutBase {

    @Nullable
    private transient UserRankResponseDto cachedUserRank = null;
    private boolean cachedUserRankValid = false;

    @Nullable
    private HorizontalLayout mainLayout;
    @Nullable
    private VerticalLayout sidebar;
    @Nullable
    private VerticalLayout contentArea;

    @Override
    public void showRouterLayoutContent(final HasElement content) {
        if (this.contentArea != null) {
            this.contentArea.getElement().removeAllChildren();
            this.contentArea.getElement().appendChild(content.getElement());
        } else {
            this.getElement().appendChild(content.getElement());
        }
    }

    @Override
    protected boolean checkSpecificPermissions(final BeforeEnterEvent event) {
        if (!this.getCachedUserRank().canAdminView()) {
            event.forwardTo(HomeView.class);
            return false;
        }
        return true;
    }

    @Override
    protected void initializeLayout() {
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

        final var leftSide = new HorizontalLayout();
        final var title = new H1("Midas Admin Panel");
        title.getStyle().set("margin", "0");
        title.getStyle().set("color", "var(--lumo-primary-text-color)");
        leftSide.add(title);

        this.rightSide = new HorizontalLayout();
        this.rightSide.setSpacing(true);
        this.rightSide.add(new ThemeToggleButton(this.themeService));

        this.topBar.add(leftSide, this.rightSide);
        this.addComponentAsFirst(this.topBar);
    }

    private void createMainLayout() {
        this.mainLayout = new HorizontalLayout();
        this.mainLayout.setSizeFull();
        this.mainLayout.setPadding(false);
        this.mainLayout.setSpacing(false);

        this.sidebar = new VerticalLayout();
        this.sidebar.setWidth("250px");
        this.sidebar.setHeightFull();
        this.sidebar.setPadding(true);
        this.sidebar.setSpacing(false);
        this.sidebar.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        this.sidebar.getStyle().set("border-right", "1px solid var(--lumo-contrast-10pct)");

        this.navigationTabs = new AdminNavigationTabs(this.getCachedUserRank());
        Objects.requireNonNull(this.sidebar).add(this.navigationTabs);

        this.contentArea = new VerticalLayout();
        this.contentArea.setSizeFull();
        this.contentArea.setPadding(false);
        this.contentArea.setSpacing(false);

        this.mainLayout.add(this.sidebar, this.contentArea);
        this.mainLayout.setFlexGrow(0, this.sidebar);
        this.mainLayout.setFlexGrow(1, this.contentArea);

        this.add(this.mainLayout);
    }

    @Override
    protected void showNavigationTabs() {
        if (this.sidebar != null) {
            this.sidebar.setVisible(true);
        }
    }

    @Override
    protected void hideNavigationTabs() {
        if (this.sidebar != null) {
            this.sidebar.setVisible(false);
        }
    }

    @Override
    protected void updateLogoutButtonVisibility() {
        if (this.authService.isAuthenticated()) {
            this.getUI().ifPresent(ui -> {
                final var location = ui.getInternals().getActiveViewLocation();
                if (location != null) {
                    final var path = location.getPath();
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

    @Nullable
    @Override
    protected Button createExtraNavigationButton() {
        return new UserViewButton(ignore -> this.getUI().ifPresent(ui -> ui.navigate(HomeView.class)));
    }

    @Override
    protected void onLogout() {
        this.cachedUserRankValid = false;
    }

    private UserRankResponseDto getCachedUserRank() {
        if (!this.cachedUserRankValid) {
            this.cachedUserRank = this.userRankService.getCurrentUserRank();
            this.cachedUserRankValid = true;
        }
        return Objects.requireNonNull(this.cachedUserRank);
    }
}
