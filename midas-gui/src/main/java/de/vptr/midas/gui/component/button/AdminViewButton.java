package de.vptr.midas.gui.component.button;

import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;

/** Button for admin view actions. */
public class AdminViewButton extends Button {
    private static final String DEFAULT_TOOLTIP = "Admin Panel";

    /** Creates a new instance with the given parameters. */
    public AdminViewButton(final ComponentEventListener<ClickEvent<Button>> viewAction, final String tooltipText) {
        super("", viewAction);
        this.setIcon(LineAwesomeIcon.TOOLS_SOLID.create());
        this.setTooltipText(tooltipText != null ? tooltipText : DEFAULT_TOOLTIP);
    }

    public AdminViewButton(final ComponentEventListener<ClickEvent<Button>> viewAction) {
        this(viewAction, null);
    }
}
