package de.vptr.midas.gui.view;

import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.dto.PageResponseDto;
import de.vptr.midas.gui.service.PageService;
import jakarta.inject.Inject;

/** PageDetailView view. */
@Route(value = "pages", layout = MainLayout.class)
public class PageDetailView extends AbstractDetailViewBase<PageResponseDto> {

    @Inject
    transient PageService pageService;

    @Override
    protected String getEntityPluralName() {
        return "Pages";
    }

    @Override
    protected String getEntitySingleName() {
        return "Page";
    }

    @Override
    protected Class<? extends Component> getBackNavigationTarget() {
        return PageView.class;
    }

    @Override
    protected void loadById(final String id) {
        this.loadDataAsync(() -> this.pageService.getPageById(id),
                optionalPage -> this.handleLoadResult(optionalPage, id));
    }

    @Override
    protected void loadByName(final String name) {
        this.loadDataAsync(() -> this.pageService.searchPages(name), pages -> {
            final Optional<PageResponseDto> optionalPage =
                    pages != null && !pages.isEmpty() ? Optional.of(pages.get(0)) : Optional.empty();
            this.handleLoadResult(optionalPage, name);
        });
    }

    @Override
    protected void renderEntity(final PageResponseDto entity) {
        final var titleHeader = new H2(entity.getTitle() != null ? entity.getTitle() : "Untitled");

        final var contentDiv = new Div();
        contentDiv.setText(entity.getContent() != null ? entity.getContent() : "");
        contentDiv.getStyle().set("white-space", "pre-wrap");
        contentDiv.getStyle().set("line-height", "1.6");
        contentDiv.getStyle().set("max-width", "800px");
        contentDiv.getStyle().set("padding", "var(--lumo-space-m)");

        this.add(titleHeader, contentDiv);
    }
}
