package de.vptr.midas.gui.view;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.PageService;
import de.vptr.midas.gui.util.NotificationUtil;
import jakarta.inject.Inject;

/** PageDetailView view. */
@Route(value = "pages", layout = MainLayout.class)
public class PageDetailView extends AbstractAsyncDataView implements BeforeEnterObserver, HasUrlParameter<String> {

    private static final Logger LOG = Logger.getLogger(PageDetailView.class);

    @Inject
    PageService pageService;

    @Inject
    AuthService authService;

    private String identifier;

    /** Initializes PageDetailView. */
    public PageDetailView() {
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

        final var backButton = new Button("← Back to Pages",
                e -> this.getUI().ifPresent(ui -> ui.navigate(PageView.class)));
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        this.add(backButton);

        this.loadPageAsync();
    }

    private void loadPageAsync() {
        if (this.identifier == null || this.identifier.trim().isEmpty()) {
            NotificationUtil.showError("No page identifier provided");
            return;
        }

        LOG.infof("Loading page with identifier: %s", this.identifier);

        // Try parsing as Long (ID) first, fall back to search by title
        final Long pageId = this.tryParseId(this.identifier);

        if (pageId != null) {
            this.loadPageById(pageId);
        } else {
            this.loadPageByTitle(this.identifier);
        }
    }

    private Long tryParseId(final String value) {
        try {
            return Long.valueOf(value);
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    private void loadPageById(final Long pageId) {
        this.loadDataAsync(() -> this.pageService.getPageById(pageId), optionalPage -> {
            if (optionalPage.isPresent()) {
                final var page = optionalPage.get();
                LOG.infof("Successfully loaded page: %s", page.getTitle());
                this.renderPage(page.getTitle(), page.getContent());
            } else {
                LOG.warnf("Page not found with ID: %s", pageId);
                this.renderNotFound();
            }
        });
    }

    private void loadPageByTitle(final String title) {
        this.loadDataAsync(
                () -> this.pageService.searchPages(title),
                pages -> {
                    if (pages != null && !pages.isEmpty()) {
                        final var page = pages.get(0);
                        LOG.infof("Found page by title search: %s", page.getTitle());
                        this.renderPage(page.getTitle(), page.getContent());
                    } else {
                        LOG.warnf("No page found for identifier: %s", title);
                        this.renderNotFound();
                    }
                });
    }

    private void renderPage(final String title, final String content) {
        final var titleHeader = new H2(title);

        final var contentDiv = new Div();
        contentDiv.setText(content != null ? content : "");
        contentDiv.getStyle().set("white-space", "pre-wrap");
        contentDiv.getStyle().set("line-height", "1.6");
        contentDiv.getStyle().set("max-width", "800px");
        contentDiv.getStyle().set("padding", "var(--lumo-space-m)");

        this.add(titleHeader, contentDiv);
    }

    private void renderNotFound() {
        final var notFoundHeader = new H2("Page Not Found");
        final var message = new Div();
        message.setText("The requested page could not be found.");
        message.getStyle().set("color", "var(--lumo-secondary-text-color)");

        this.add(notFoundHeader, message);
    }
}
