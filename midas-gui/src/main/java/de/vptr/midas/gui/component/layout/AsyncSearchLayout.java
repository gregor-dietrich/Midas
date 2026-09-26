package de.vptr.midas.gui.component.layout;

import java.util.function.Consumer;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;

import de.vptr.midas.gui.component.button.SearchButton;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;

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
    private final transient SearchExecutionSupport searchExecutionSupport;

    public AsyncSearchLayout(final Consumer<String> searchAction, final String placeholder, final String tooltip) {
        this(searchAction, () -> {
        }, placeholder, tooltip, DEFAULT_WIDTH, new SearchExecutionSupport());
    }

    public AsyncSearchLayout(final Consumer<String> searchAction, @Nullable final Runnable clearAction,
            final String placeholder, final String tooltip) {
        this(searchAction, clearAction != null ? clearAction : () -> {
        }, placeholder, tooltip, DEFAULT_WIDTH, new SearchExecutionSupport());
    }

    /** Initializes AsyncSearchLayout. */
    public AsyncSearchLayout(final Consumer<String> searchAction, @Nullable final Runnable clearAction,
            final String placeholder, final String tooltip, final String fieldWidth) {
        this(searchAction, clearAction != null ? clearAction : () -> {
        }, placeholder, tooltip, fieldWidth, new SearchExecutionSupport());
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

        this.clearButton = new Button(VaadinIcon.CLOSE.create(), e -> this.executeClear());
        this.clearButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        this.clearButton.setTooltipText("Clear search");

        this.add(this.textField, this.searchButton, this.clearButton);
    }

    public AsyncSearchLayout(final Consumer<String> searchAction, @Nullable final Runnable clearAction) {
        this(searchAction, clearAction != null ? clearAction : () -> {
        }, DEFAULT_PLACEHOLDER, "Search");
    }

    private void executeSearch() {
        this.searchExecutionSupport.execute(this.textField.getValue(), this.textField::setValue, this.searchAction);
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

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Expose TextField intentionally so parent views can use it")
    public TextField getTextfield() {
        return this.textField;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Expose search button intentionally so parent views can use it")
    public Button getSearchButton() {
        return this.searchButton;
    }

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Expose clear button intentionally so parent views can use it")
    public Button getClearButton() {
        return this.clearButton;
    }

    public String getValue() {
        return this.textField.getValue();
    }
}
