package de.vptr.midas.gui.component.button;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

/** Button for logout actions. */
public class LogoutButton extends Button {
    private static final String DEFAULT_TOOLTIP = "Logout";

    /** Creates a new instance with the given parameters. */
    public LogoutButton(final ComponentEventListener<ClickEvent<Button>> logoutAction, final String tooltipText) {
        super("", logoutAction);
        this.addThemeVariants(ButtonVariant.LUMO_ERROR);
        this.setIcon(new Icon(VaadinIcon.SIGN_OUT));
        this.setTooltipText(tooltipText != null ? tooltipText : DEFAULT_TOOLTIP);
    }

    public LogoutButton(final ComponentEventListener<ClickEvent<Button>> logoutAction) {
        this(logoutAction, DEFAULT_TOOLTIP);
    }
}
