package de.vptr.midas.gui.component.layout;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;

import de.vptr.midas.gui.component.button.FilterButton;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/** Layout for integer filter. */
public class IntegerFilterLayout extends HorizontalLayout {

    private static final String DEFAULT_LABEL = "Filter";
    private static final String DEFAULT_TOOLTIP = "Filter";
    private static final String DEFAULT_WIDTH = "150px";

    private final Button button;
    private final IntegerField integerField;

    /** Initializes IntegerFilterLayout. */
    public IntegerFilterLayout(final ComponentEventListener<ClickEvent<Button>> filterAction, final String tooltipText,
            final String fieldLabel, final String fieldWidth) {
        this.setAlignItems(Alignment.END);
        this.setSpacing(true);

        this.integerField = new IntegerField(fieldLabel);
        this.integerField.setWidth(fieldWidth);

        this.button = new FilterButton(filterAction, tooltipText);

        this.add(this.integerField, this.button);
    }

    public IntegerFilterLayout(final ComponentEventListener<ClickEvent<Button>> filterAction, final String tooltipText,
            final String fieldLabel) {
        this(filterAction, tooltipText, fieldLabel, DEFAULT_WIDTH);
    }

    public IntegerFilterLayout(final ComponentEventListener<ClickEvent<Button>> filterAction,
            final String tooltipText) {
        this(filterAction, tooltipText, DEFAULT_LABEL);
    }

    public IntegerFilterLayout(final ComponentEventListener<ClickEvent<Button>> filterAction) {
        this(filterAction, DEFAULT_TOOLTIP);
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Expose button intentionally so parent views can use it")
    public Button getButton() {
        return this.button;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Expose integer field intentionally so parent views can use it")
    public IntegerField getIntegerField() {
        return this.integerField;
    }
}
