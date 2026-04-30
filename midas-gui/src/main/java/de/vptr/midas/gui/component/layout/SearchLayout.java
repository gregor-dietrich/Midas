package de.vptr.midas.gui.component.layout;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;

import de.vptr.midas.gui.component.button.SearchButton;

/** Layout for search. */
public class SearchLayout extends HorizontalLayout {

    private static final String DEFAULT_LABEL = "Search";
    private static final String DEFAULT_PLACEHOLDER = "Search...";
    private static final String DEFAULT_TOOLTIP = "Search";
    private static final String DEFAULT_WIDTH = "300px";

    private final ComponentEventListener<ClickEvent<Button>> searchAction;
    private final SearchExecutionSupport searchExecutionSupport;
    private Button button;
    private final TextField textField;

    /** Initializes SearchLayout. */
    public SearchLayout(final HasValue.ValueChangeListener<ComponentValueChangeEvent<TextField, String>> listener,
            final ComponentEventListener<ClickEvent<Button>> searchAction, final String placeholderText,
            final String tooltipText, final String fieldLabel, final String fieldWidth) {
        this(listener, searchAction, placeholderText, tooltipText, fieldLabel, fieldWidth, new SearchExecutionSupport());
    }

    SearchLayout(final HasValue.ValueChangeListener<ComponentValueChangeEvent<TextField, String>> listener,
            final ComponentEventListener<ClickEvent<Button>> searchAction, final String placeholderText,
            final String tooltipText, final String fieldLabel, final String fieldWidth,
            final SearchExecutionSupport searchExecutionSupport) {
        this.searchAction = searchAction;
        this.searchExecutionSupport = searchExecutionSupport;

        this.setAlignItems(Alignment.END);
        this.setSpacing(true);

        this.textField = new TextField(fieldLabel);
        this.textField.setClearButtonVisible(true);
        this.textField.setPlaceholder(placeholderText);
        this.textField.setWidth(fieldWidth);
        this.textField.setMaxLength(SearchExecutionSupport.MAX_QUERY_LENGTH);
        this.textField.addValueChangeListener(listener);

        this.textField.addKeyPressListener(Key.ENTER, event -> this.executeSearch());

        this.button = new SearchButton(_ -> this.executeSearch(), tooltipText);

        this.add(this.textField, this.button);
    }

    public SearchLayout(final HasValue.ValueChangeListener<ComponentValueChangeEvent<TextField, String>> listener,
            final ComponentEventListener<ClickEvent<Button>> searchAction, final String placeholderText,
            final String tooltipText, final String fieldLabel) {
        this(listener, searchAction, placeholderText, tooltipText, fieldLabel, DEFAULT_WIDTH);
    }

    public SearchLayout(final HasValue.ValueChangeListener<ComponentValueChangeEvent<TextField, String>> listener,
            final ComponentEventListener<ClickEvent<Button>> searchAction, final String placeholderText,
            final String tooltipText) {
        this(listener, searchAction, placeholderText, tooltipText, DEFAULT_LABEL);
    }

    public SearchLayout(final HasValue.ValueChangeListener<ComponentValueChangeEvent<TextField, String>> listener,
            final ComponentEventListener<ClickEvent<Button>> searchAction, final String placeholderText) {
        this(listener, searchAction, placeholderText, DEFAULT_TOOLTIP);
    }

    public SearchLayout(final HasValue.ValueChangeListener<ComponentValueChangeEvent<TextField, String>> listener,
            final ComponentEventListener<ClickEvent<Button>> searchAction) {
        this(listener, searchAction, DEFAULT_PLACEHOLDER);
    }

    private void executeSearch() {
        if (this.searchAction == null) {
            return;
        }

        final String query = this.searchExecutionSupport.normalize(this.textField.getValue());
        if (query == null) {
            return;
        }

        if (!query.equals(this.textField.getValue())) {
            this.textField.setValue(query);
        }

        if (this.searchExecutionSupport.shouldExecute(query)) {
            this.searchAction.onComponentEvent(new ClickEvent<>(this.button));
        }
    }

    public Button getButton() {
        return this.button;
    }

    public TextField getTextfield() {
        return this.textField;
    }
}
