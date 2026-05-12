package de.vptr.midas.gui.view;

import java.util.Objects;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.component.button.CreateButton;
import de.vptr.midas.gui.component.button.DeleteButton;
import de.vptr.midas.gui.component.button.EditButton;
import de.vptr.midas.gui.component.button.RefreshButton;
import de.vptr.midas.gui.component.dialog.FormDialog;
import de.vptr.midas.gui.component.layout.SearchLayout;
import de.vptr.midas.gui.dto.PageDto;
import de.vptr.midas.gui.dto.PageResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.PageService;
import de.vptr.midas.gui.util.MidasErrorHandler;
import de.vptr.midas.gui.util.NotificationUtil;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** Admin view for managing pages. */
@Route(value = "admin/pages", layout = AdminMainLayout.class)
public class AdminPageView extends AbstractAsyncDataView implements BeforeEnterObserver {

    private static final Logger LOG = Logger.getLogger(AdminPageView.class);

    @Inject
    transient PageService pageService;

    @Inject
    transient AuthService authService;

    @Nullable
    private Grid<PageResponseDto> grid;
    @Nullable
    private TextField searchField;
    @Nullable
    private Button searchButton;

    @Nullable
    private Dialog pageDialog;
    @Nullable
    private Binder<PageDto> binder;
    @Nullable
    private transient PageDto currentPage;

    /** Initializes AdminPageView. */
    public AdminPageView() {
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
        this.setupLazyDataProvider();
    }

    private void setupLazyDataProvider() {
        Objects.requireNonNull(this.grid)
                .setItems(query -> this.pageService.getAllPages(query.getPage(), query.getPageSize()).stream());
    }

    private void buildUi() {
        this.removeAll();

        final var header = new H2("Pages");
        final var searchLayout = this.createSearchLayout();
        final var buttonLayout = this.createButtonLayout();
        this.createGrid();
        this.pageDialog = new FormDialog();

        this.add(header, searchLayout, buttonLayout, Objects.requireNonNull(this.grid));
    }

    private HorizontalLayout createSearchLayout() {
        final var searchLayout = new SearchLayout(e -> {
            if (e.getValue() == null || e.getValue().trim().isEmpty()) {
                this.setupLazyDataProvider();
            }
        }, ignore -> this.searchPages(), "Search by title or content...", "Search Pages");

        this.searchButton = searchLayout.getButton();
        this.searchField = searchLayout.getTextfield();

        return searchLayout;
    }

    private HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var createButton = new CreateButton(ignore -> this.openPageDialog(null));
        final var refreshButton = new RefreshButton(ignore -> this.setupLazyDataProvider());

        layout.add(createButton, refreshButton);
        return layout;
    }

    private void createGrid() {
        this.grid = new Grid<>(PageResponseDto.class, false);
        this.grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.grid.setSizeFull();

        // Configure columns
        this.grid.addColumn((ValueProvider<PageResponseDto, ?>) PageResponseDto::getId).setHeader("ID").setWidth("80px")
                .setFlexGrow(0);

        // Make the title column clickable
        this.grid.addComponentColumn(page -> {
            final var titleSpan = new Span(page.getTitle());
            titleSpan.getStyle().set("color", "var(--lumo-primary-text-color)");
            titleSpan.getStyle().set("cursor", "pointer");
            titleSpan.getStyle().set("width", "100%");
            titleSpan.getStyle().set("display", "block");
            titleSpan.addClickListener(ignore -> this.openPageDialog(page.toPageDto()));
            return titleSpan;
        }).setHeader("Title").setFlexGrow(2);

        // Add action column
        this.grid.addComponentColumn(this::createActionButtons).setHeader("Actions").setWidth("150px").setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(final PageResponseDto page) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var editButton = new EditButton(ignore -> this.openPageDialog(page.toPageDto()));
        final var deleteButton = new DeleteButton(ignore -> this.deletePage(page.toPageDto()));

        layout.add(editButton, deleteButton);
        return layout;
    }

    private void openPageDialog(@Nullable final PageDto page) {
        final var pd = Objects.requireNonNull(this.pageDialog);
        pd.removeAll();
        this.currentPage = page != null ? page : new PageDto();

        this.binder = new Binder<>(PageDto.class);

        final var title = new H3(page != null ? "Edit Page" : "Create Page");

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        // Form fields
        final var titleField = new TextField("Title");
        titleField.setRequired(true);
        titleField.setWidthFull();
        titleField.setMaxLength(255);
        titleField.setInvalid(false); // Clear any previous validation state

        final var contentField = new TextArea("Content");
        contentField.setRequired(true);
        contentField.setWidthFull();
        contentField.setHeight("250px");
        contentField.setMaxLength(50_000);
        contentField.setInvalid(false); // Clear any previous validation state

        // Bind fields
        this.binder.forField(titleField)
                .withValidator(value -> value != null && !value.trim().isEmpty(), "Title is required")
                .bind(PageDto::getTitle, PageDto::setTitle);
        this.binder.forField(contentField)
                .withValidator(value -> value != null && !value.trim().isEmpty(), "Content is required")
                .bind(PageDto::getContent, PageDto::setContent);

        form.add(titleField, contentField);

        // Button layout
        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var saveButton = new Button("Save", ignore -> this.savePage());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var cancelButton = new Button("Cancel", ignore -> pd.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttonLayout.add(saveButton, cancelButton);

        final var dialogLayout = new VerticalLayout(title, form, buttonLayout);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);
        dialogLayout.setSizeFull();

        pd.add(dialogLayout);

        // Load current page data
        this.binder.readBean(this.currentPage);

        pd.open();
    }

    private void savePage() {
        try {
            final var b = Objects.requireNonNull(this.binder);
            final var cp = Objects.requireNonNull(this.currentPage);
            // Validate the form before attempting to save
            if (!b.validate().isOk()) {
                NotificationUtil.showError("Please check the form for errors");
                return;
            }

            b.writeBean(cp);

            if (cp.getId() == null) {
                this.pageService.createPage(cp);
                NotificationUtil.showSuccess("Page created successfully");
            } else {
                this.pageService.updatePage(cp);
                NotificationUtil.showSuccess("Page updated successfully");
            }

            Objects.requireNonNull(this.pageDialog).close();
            this.setupLazyDataProvider();

        } catch (final ValidationException e) {
            NotificationUtil.showError("Please check the form for errors");
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        } catch (final ServiceException e) {
            NotificationUtil.showError(e, "Saving page failed. Please try again.");
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error saving page", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void deletePage(final PageDto page) {
        try {
            this.pageService.deletePage(Objects.requireNonNull(page.getId()));
            NotificationUtil.showSuccess("Page deleted successfully");
            this.setupLazyDataProvider();
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        } catch (final ServiceException e) {
            NotificationUtil.showError(e, "Deleting page failed. Please try again.");
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error deleting page", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void searchPages() {
        final String query = Objects.requireNonNull(this.searchField).getValue();
        if (query == null || query.trim().isEmpty()) {
            NotificationUtil.showWarning("Please enter a search query");
            return;
        }

        final var sb = Objects.requireNonNull(this.searchButton);
        sb.setEnabled(false);
        sb.setText("Searching...");

        this.loadDataAsync(() -> this.pageService.searchPages(query.trim()), pages -> {
            this.resetSearchButton();
            LOG.infof("Search found %s pages", pages.size());
            Objects.requireNonNull(this.grid).setItems(pages);
        }, this::handleSearchError);
    }

    void handleSearchError(final Throwable throwable) {
        this.resetSearchButton();
        MidasErrorHandler.handleInUiContext(throwable);
    }

    private void resetSearchButton() {
        Objects.requireNonNull(this.searchButton).setEnabled(true);
        this.searchButton.setText("Search Pages");
    }
}
