package de.vptr.midas.gui.view;

import java.util.Objects;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.component.layout.AsyncSearchLayout;
import de.vptr.midas.gui.dto.PageResponseDto;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.PageService;
import de.vptr.midas.gui.util.MidasErrorHandler;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** User view for browsing pages. */
@Route(value = "pages", layout = MainLayout.class)
public class PageView extends AbstractAsyncDataView implements BeforeEnterObserver {

    private static final Logger LOG = Logger.getLogger(PageView.class);

    @Inject
    transient PageService pageService;

    @Inject
    transient AuthService authService;

    @Nullable
    private Grid<PageResponseDto> grid;
    @Nullable
    private AsyncSearchLayout searchLayout;

    /** Initializes PageView. */
    public PageView() {
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

        this.buildUi();
        this.loadPagesAsync();
    }

    private void loadPagesAsync() {
        LOG.info("Starting async page loading");

        this.loadDataAsync(() -> this.pageService.getAllPages(), pages -> {
            LOG.infof("Successfully loaded %s pages", pages.size());
            Objects.requireNonNull(this.grid).setItems(pages);
        });
    }

    private void buildUi() {
        this.removeAll();

        final var header = new H2("Pages");
        this.searchLayout = new AsyncSearchLayout(this::searchPages, this::loadPagesAsync,
                "Search by title or content...", "Search Pages");
        this.createGrid();

        this.add(header, this.searchLayout, Objects.requireNonNull(this.grid));
    }

    private void createGrid() {
        this.grid = new Grid<>(PageResponseDto.class, false);
        this.grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.grid.setSizeFull();
        this.grid.setPageSize(20);

        // Clickable title column that navigates to detail view
        this.grid.addComponentColumn(page -> {
            final var titleSpan = new Span(page.getTitle());
            titleSpan.getStyle().set("color", "var(--lumo-primary-text-color)");
            titleSpan.getStyle().set("cursor", "pointer");
            titleSpan.getStyle().set("width", "100%");
            titleSpan.getStyle().set("display", "block");
            titleSpan.addClickListener(_ -> this.getUI().ifPresent(ui -> ui.navigate("pages/" + page.getId())));
            return titleSpan;
        }).setHeader("Title").setFlexGrow(1);

        // Content preview column (first 100 chars)
        this.grid.addColumn(page -> {
            final var content = page.getContent();
            if (content == null || content.isEmpty()) {
                return "";
            }
            return content.length() > 100 ? content.substring(0, 100) + "..." : content;
        }).setHeader("Content").setFlexGrow(2);
    }

    private void searchPages(final String query) {
        final var sl = Objects.requireNonNull(this.searchLayout);
        sl.setSearching(true);

        this.loadDataAsync(() -> this.pageService.searchPages(query), pages -> {
            sl.setSearching(false);
            LOG.infof("Search found %s pages", pages.size());
            Objects.requireNonNull(this.grid).setItems(pages);
        }, throwable -> {
            sl.setSearching(false);
            MidasErrorHandler.handleInUiContext(throwable);
        });
    }
}
