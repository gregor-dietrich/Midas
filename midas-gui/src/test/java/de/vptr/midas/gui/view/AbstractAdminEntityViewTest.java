package de.vptr.midas.gui.view;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.vptr.midas.gui.util.NotificationUtil;

class AbstractAdminEntityViewTest extends AbstractUiTestBase {

    private TestAdminEntityView view;

    private static final class TestAdminEntityView extends AbstractAdminEntityView<String> {
        @Override
        protected void onBeforeEnterAuthenticated() {
        }

        @Override
        protected String getViewTitle() {
            return "Test";
        }

        @Override
        protected Class<String> getEntityClass() {
            return String.class;
        }

        @Override
        protected void configureGrid(Grid<String> grid) {
        }

        @Override
        protected void initAdditionalComponents() {
        }

        @Override
        protected void setupLazyDataProvider() {
        }

        @Override
        protected void executeSearch() {
        }

        @Override
        protected String getSearchPlaceholder() {
            return "";
        }

        @Override
        protected String getSearchButtonText() {
            return "";
        }

        @Override
        protected HorizontalLayout createButtonLayout() {
            return new HorizontalLayout();
        }

        @Override
        protected String getEntityNameSingle() {
            return "Test";
        }

        @Override
        protected void executeDateRangeFilter(LocalDate startDate, LocalDate endDate) {
        }

        @Override
        protected void executeUserFilter(Integer userId) {
        }

        void triggerFilterByDateRange() {
            this.filterByDateRange();
        }

        void setPickers(DatePicker start, DatePicker end) {
            this.startDatePicker = start;
            this.endDatePicker = end;
        }
    }

    @BeforeEach
    void setUp() {
        this.view = new TestAdminEntityView();
    }

    @Test
    void filterByDateRange_shouldShowWarning_whenDatesAreNull() {
        final var start = new DatePicker();
        final var end = new DatePicker();
        this.view.setPickers(start, end);

        this.view.triggerFilterByDateRange();

        this.notificationMock.verify(() -> NotificationUtil.showWarning("Please select both start and end dates"));
    }

    @Test
    void filterByDateRange_shouldShowWarning_whenStartDateAfterEndDate() {
        final var start = new DatePicker();
        start.setValue(LocalDate.now(ZoneId.systemDefault()).plusDays(1));
        final var end = new DatePicker();
        end.setValue(LocalDate.now(ZoneId.systemDefault()));
        this.view.setPickers(start, end);

        this.view.triggerFilterByDateRange();

        this.notificationMock.verify(() -> NotificationUtil.showWarning("Start date must be before end date"));
    }
}
