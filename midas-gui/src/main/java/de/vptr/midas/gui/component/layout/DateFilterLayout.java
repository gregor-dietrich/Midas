package de.vptr.midas.gui.component.layout;

import java.time.LocalDate;
import java.time.ZoneId;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import de.vptr.midas.gui.component.button.FilterButton;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/** Layout for date filter. */
public class DateFilterLayout extends HorizontalLayout {

    private static final String DEFAULT_TOOLTIP = "Filter by Date";
    private static final String DEFAULT_WIDTH = "150px";

    private final Button button;
    private final DatePicker startDatePicker;
    private final DatePicker endDatePicker;

    /** Initializes DateFilterLayout. */
    public DateFilterLayout(final ComponentEventListener<ClickEvent<Button>> filterAction, final String tooltipText,
            final String fieldWidth) {
        this.setAlignItems(Alignment.END);
        this.setSpacing(true);

        this.startDatePicker = new DatePicker("Start Date");
        this.startDatePicker.setWidth(fieldWidth);
        this.startDatePicker.setValue(LocalDate.of(1970, 1, 1));

        this.endDatePicker = new DatePicker("End Date");
        this.endDatePicker.setWidth(fieldWidth);
        this.endDatePicker.setValue(LocalDate.now(ZoneId.systemDefault()));

        this.button = new FilterButton(filterAction, tooltipText);

        this.add(this.startDatePicker, this.endDatePicker, this.button);
    }

    public DateFilterLayout(final ComponentEventListener<ClickEvent<Button>> filterAction, final String tooltipText) {
        this(filterAction, tooltipText, DEFAULT_WIDTH);
    }

    public DateFilterLayout(final ComponentEventListener<ClickEvent<Button>> filterAction) {
        this(filterAction, DEFAULT_TOOLTIP);
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Expose button intentionally so parent views can use it")
    public Button getButton() {
        return this.button;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Expose start date picker intentionally so parent views can use it")
    public DatePicker getStartDatePicker() {
        return this.startDatePicker;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Expose end date picker intentionally so parent views can use it")
    public DatePicker getEndDatePicker() {
        return this.endDatePicker;
    }
}
