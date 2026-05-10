package de.vptr.midas.gui.component.layout;

import java.util.function.Consumer;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;

import de.vptr.midas.gui.component.button.SearchButton;

/**
 * Reusable search layout with text field, search button, and clear button. Includes built-in search-in-progress state
 * management.
 */
public class AsyncSearchLayout extends HorizontalLayout {

    private static final String DEFAULT_PLACEHOLDER = "Search...";
    private static final String DEFAULT_WIDTH = "300px";

    private final TextField textField;
    private final Button searchButton;
    private final Button clearButton;
    private final Consumer<String> searchAction;
    private final Runnable clearAction;
    private final SearchExecutionSupport searchExecutionSupport;

    public AsyncSearchLayout(final Consumer<String> searchAction, final Runnable clearAction, final String placeholder,
            final String tooltip) {
        this(searchAction, clearAction, placeholder, tooltip, DEFAULT_WIDTH, new SearchExecutionSupport());
    }

    /** Initializes AsyncSearchLayout. */
    public AsyncSearchLayout(final Consumer<String> searchAction, final Runnable clearAction, final String placeholder,
            final String tooltip, final String fieldWidth) {
        this(searchAction, clearAction, placeholder, tooltip, fieldWidth, new SearchExecutionSupport());
    }

    AsyncSearchLayout(final Consumer<String> searchAction, final Runnable clearAction, final String placeholder,
            final String tooltip, final String fieldWidth, final SearchExecutionSupport searchExecutionSupport) {
        this.searchAction = searchAction;
        this.clearAction = clearAction;
        this.searchExecutionSupport = searchExecutionSupport;

        this.setAlignItems(Alignment.END);
        this.setSpacing(true);

        this.textField = new TextField();
        this.textField.setPlaceholder(placeholder);
        this.textField.setWidth(fieldWidth);
        this.textField.setMaxLength(SearchExecutionSupport.MAX_QUERY_LENGTH);
        this.textField.setClearButtonVisible(true);
        this.textField.addKeyPressListener(Key.ENTER, event -> this.executeSearch());

        this.searchButton = new SearchButton(e -> this.executeSearch(), tooltip);

        this.clearButton = new Button(VaadinIcon.CLOSE_SMALL.create(), e -> this.executeClear());
        this.clearButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        this.clearButton.setTooltipText("Clear search");

        this.add(this.textField, this.searchButton, this.clearButton);
    }

    public AsyncSearchLayout(final Consumer<String> searchAction, final Runnable clearAction) {
        this(searchAction, clearAction, DEFAULT_PLACEHOLDER, "Search");
    }

    private void executeSearch() {
        final String query = this.searchExecutionSupport.normalize(this.textField.getValue());
        if (query == null) {
            return;
        }

        if (!query.equals(this.textField.getValue())) {
            this.textField.setValue(query);
        }

        if (this.searchExecutionSupport.shouldExecute(query)) {
            this.searchAction.accept(query);
        }
    }

    private void executeClear() {
        this.textField.clear();
        this.clearAction.run();
    }

    /** Sets the search-in-progress visual state. */
    public void setSearching(final boolean searching) {
        this.searchButton.setEnabled(!searching);
        this.textField.setEnabled(!searching);
        this.clearButton.setEnabled(!searching);
    }

    public TextField getTextField() {
        return this.textField;
    }

    public Button getSearchButton() {
        return this.searchButton;
    }

    public Button getClearButton() {
        return this.clearButton;
    }

    public String getValue() {
        return this.textField.getValue();
    }
}
