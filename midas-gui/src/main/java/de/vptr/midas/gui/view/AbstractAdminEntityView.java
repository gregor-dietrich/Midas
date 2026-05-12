package de.vptr.midas.gui.view;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

import de.vptr.midas.gui.component.layout.DateFilterLayout;
import de.vptr.midas.gui.component.layout.IntegerFilterLayout;
import de.vptr.midas.gui.component.layout.SearchLayout;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.util.MidasErrorHandler;
import de.vptr.midas.gui.util.NotificationUtil;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** Abstract base class for admin entity management views. */
public abstract class AbstractAdminEntityView<T> extends AbstractAsyncDataView implements BeforeEnterObserver {

    private final Logger log = Logger.getLogger(this.getClass());

    @Inject
    protected transient AuthService authService;

    @Nullable
    protected Grid<T> grid;
    @Nullable
    protected TextField searchField;
    @Nullable
    protected Button searchButton;
    @Nullable
    protected DatePicker startDatePicker;
    @Nullable
    protected DatePicker endDatePicker;
    @Nullable
    protected IntegerField userIdField;

    protected AbstractAdminEntityView() {
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
        this.onBeforeEnterAuthenticated();
    }

    protected abstract void onBeforeEnterAuthenticated();

    protected void buildUi() {
        this.removeAll();

        final var header = new H2(this.getViewTitle());
        final var searchLayout = this.createSearchLayout();
        final var buttonLayout = this.createButtonLayout();

        this.grid = new Grid<>(this.getEntityClass(), false);
        this.grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.grid.setSizeFull();
        this.configureGrid(this.grid);

        this.initAdditionalComponents();

        this.add(header, searchLayout, buttonLayout, Objects.requireNonNull(this.grid));
    }

    protected abstract String getViewTitle();

    protected abstract Class<T> getEntityClass();

    protected abstract void configureGrid(Grid<T> grid);

    /**
     * Override to add extra UI components after the grid; called once per {@link #buildUi()}. Default is a no-op.
     */
    protected abstract void initAdditionalComponents();

    protected HorizontalLayout createSearchLayout() {
        final var searchLayout = new SearchLayout(e -> {
            if (e.getValue() == null || e.getValue().trim().isEmpty()) {
                this.setupLazyDataProvider();
            }
        }, ignore -> this.executeSearch(), this.getSearchPlaceholder(), this.getSearchButtonText());

        this.searchButton = searchLayout.getButton();
        this.searchField = searchLayout.getTextfield();

        this.addCustomFilterLayouts(searchLayout);

        return searchLayout;
    }

    protected abstract void setupLazyDataProvider();

    protected abstract void executeSearch();

    protected abstract String getSearchPlaceholder();

    protected abstract String getSearchButtonText();

    protected void addCustomFilterLayouts(final SearchLayout searchLayout) {
        if (this.hasDateAndUserFilters()) {
            final var dateFilterLayout = new DateFilterLayout(ignore -> this.filterByDateRange());
            this.startDatePicker = dateFilterLayout.getStartDatePicker();
            this.endDatePicker = dateFilterLayout.getEndDatePicker();

            final var userFilterLayout =
                    new IntegerFilterLayout(ignore -> this.filterByUser(), "Enter User ID...", "Filter by User");
            this.userIdField = userFilterLayout.getIntegerField();

            this.appendExtraFilters(searchLayout, dateFilterLayout, userFilterLayout);
        }
    }

    protected void appendExtraFilters(final SearchLayout searchLayout, final DateFilterLayout dateFilterLayout,
            final IntegerFilterLayout userFilterLayout) {
        searchLayout.add(dateFilterLayout, userFilterLayout);
    }

    protected boolean hasDateAndUserFilters() {
        return true;
    }

    protected abstract HorizontalLayout createButtonLayout();

    protected void filterByDateRange() {
        final LocalDate startDate = Objects.requireNonNull(this.startDatePicker).getValue();
        final LocalDate endDate = Objects.requireNonNull(this.endDatePicker).getValue();

        if (startDate == null || endDate == null) {
            NotificationUtil.showWarning("Please select both start and end dates");
            return;
        }

        if (startDate.isAfter(endDate)) {
            NotificationUtil.showWarning("Start date must be before end date");
            return;
        }

        this.executeDateRangeFilter(startDate, endDate);
    }

    /**
     * Called when the user triggers a date-range filter. Only invoked when {@link #hasDateAndUserFilters()} returns
     * {@code true}. Default is a no-op; override in subclasses that enable date filtering.
     */
    protected abstract void executeDateRangeFilter(LocalDate startDate, LocalDate endDate);

    protected void filterByUser() {
        final Integer userId = Objects.requireNonNull(this.userIdField).getValue();
        if (userId == null) {
            NotificationUtil.showWarning("Please enter a user ID");
            return;
        }

        this.executeUserFilter(userId);
    }

    /**
     * Called when the user triggers a user-ID filter. Only invoked when {@link #hasDateAndUserFilters()} returns
     * {@code true}. Default is a no-op; override in subclasses that enable user filtering.
     */
    protected abstract void executeUserFilter(Integer userId);

    protected void handleDeletion(final Runnable deleteAction) {
        try {
            deleteAction.run();
            NotificationUtil.showSuccess(this.getEntityNameSingle() + " deleted successfully");
            this.setupLazyDataProvider();
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        } catch (final ServiceException e) {
            NotificationUtil.showError(e,
                    "Deleting " + this.getEntityNameSingle().toLowerCase(Locale.ROOT) + " failed. Please try again.");
        } catch (final RuntimeException e) {
            this.log.error("Unexpected error deleting " + this.getEntityNameSingle().toLowerCase(Locale.ROOT), e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    protected abstract String getEntityNameSingle();

    protected void executeAsyncSearch(final String query, final Function<String, List<T>> searchFunction) {
        if (query == null || query.trim().isEmpty()) {
            NotificationUtil.showWarning("Please enter a search query");
            return;
        }
        final var sb = Objects.requireNonNull(this.searchButton);
        sb.setEnabled(false);
        sb.setText("Searching...");

        this.loadDataAsync(() -> searchFunction.apply(query), items -> {
            sb.setEnabled(true);
            sb.setText(this.getSearchButtonText());
            Objects.requireNonNull(this.grid).setItems(items);
        }, throwable -> {
            sb.setEnabled(true);
            sb.setText(this.getSearchButtonText());
            MidasErrorHandler.handleInUiContext(throwable);
        });
    }

    protected Runnable getAuthFailureRunnable() {
        return () -> this.getUI().ifPresent(ui -> ui.navigate(LoginView.class));
    }
}
