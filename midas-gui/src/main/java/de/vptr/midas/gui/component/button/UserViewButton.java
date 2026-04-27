package de.vptr.midas.gui.component.button;

import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;

/** Button for user view actions. */
public class UserViewButton extends Button {
    private static final String DEFAULT_TOOLTIP = "Return to Homepage";

    /** Creates a new instance with the given parameters. */
    public UserViewButton(final ComponentEventListener<ClickEvent<Button>> viewAction, final String tooltipText) {
        super("", viewAction);
        this.setIcon(LineAwesomeIcon.UNDO_SOLID.create());
        this.setTooltipText(tooltipText != null ? tooltipText : DEFAULT_TOOLTIP);
    }

    public UserViewButton(final ComponentEventListener<ClickEvent<Button>> viewAction) {
        this(viewAction, null);
    }
}
