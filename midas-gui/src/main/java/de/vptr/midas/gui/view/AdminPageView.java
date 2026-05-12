package de.vptr.midas.gui.view;

import java.time.LocalDate;
import java.util.Objects;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.component.button.CreateButton;
import de.vptr.midas.gui.component.button.DeleteButton;
import de.vptr.midas.gui.component.button.EditButton;
import de.vptr.midas.gui.component.button.RefreshButton;
import de.vptr.midas.gui.component.dialog.AdminPageDialog;
import de.vptr.midas.gui.dto.PageDto;
import de.vptr.midas.gui.dto.PageResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.service.PageService;
import de.vptr.midas.gui.util.NotificationUtil;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** Admin view for managing pages. */
@Route(value = "admin/pages", layout = AdminMainLayout.class)
public class AdminPageView extends AbstractAdminEntityView<PageResponseDto> {

    @Inject
    transient PageService pageService;

    @Nullable
    private AdminPageDialog pageDialog;

    @Override
    protected void onBeforeEnterAuthenticated() {
        this.setupLazyDataProvider();
    }

    @Override
    protected String getViewTitle() {
        return "Pages";
    }

    @Override
    protected Class<PageResponseDto> getEntityClass() {
        return PageResponseDto.class;
    }

    @Override
    protected String getEntityNameSingle() {
        return "Page";
    }

    @Override
    protected String getSearchPlaceholder() {
        return "Search by title or content...";
    }

    @Override
    protected String getSearchButtonText() {
        return "Search Pages";
    }

    @Override
    protected boolean hasDateAndUserFilters() {
        return false;
    }

    @Override
    protected void initAdditionalComponents() {
        this.pageDialog =
                new AdminPageDialog(this.pageService, this::setupLazyDataProvider, this.getAuthFailureRunnable());
    }

    @Override
    protected void setupLazyDataProvider() {
        Objects.requireNonNull(this.grid)
                .setItems(query -> this.pageService.getAllPages(query.getPage(), query.getPageSize()).stream());
    }

    @Override
    protected void executeSearch() {
        final String query = Objects.requireNonNull(this.searchField).getValue();
        this.executeAsyncSearch(query, q -> this.pageService.searchPages(q.trim()));
    }

    @Override
    protected HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);
        layout.add(new CreateButton(ignore -> this.openPageDialog(null)),
                new RefreshButton(ignore -> this.setupLazyDataProvider()));
        return layout;
    }

    @Override
    protected void configureGrid(final Grid<PageResponseDto> g) {
        g.addColumn((ValueProvider<PageResponseDto, ?>) PageResponseDto::getId).setHeader("ID").setWidth("80px")
                .setFlexGrow(0);

        g.addComponentColumn(
                page -> this.createClickableSpan(page.getTitle(), ignore -> this.openPageDialog(page.toPageDto())))
                .setHeader("Title").setFlexGrow(2);

        g.addComponentColumn(this::createActionButtons).setHeader("Actions").setWidth("150px").setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(final PageResponseDto page) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var editButton = new EditButton(ignore -> this.openPageDialog(page.toPageDto()));
        final var deleteButton = new DeleteButton(ignore -> this.handleDeletion(() -> {
            this.pageService.deletePage(Objects.requireNonNull(page.getId()));
        }));

        layout.add(editButton, deleteButton);
        return layout;
    }

    private void openPageDialog(@Nullable final PageDto page) {
        Objects.requireNonNull(this.pageDialog).open(page);
    }

    void handleSearchError(final RuntimeException e) {
        if (this.searchButton != null) {
            this.searchButton.setEnabled(true);
            this.searchButton.setText("Search Pages");
        }
        if (e instanceof AuthenticationException) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        }
    }

    @Override
    protected void executeDateRangeFilter(final LocalDate startDate, final LocalDate endDate) {
        // Not used: hasDateAndUserFilters() returns false
    }

    @Override
    protected void executeUserFilter(final Integer userId) {
        // Not used: hasDateAndUserFilters() returns false
    }
}
