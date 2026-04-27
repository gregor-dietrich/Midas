package de.vptr.midas.gui.component.button;

import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;

/** Button for add comment actions. */
public class AddCommentButton extends Button {
    private static final String DEFAULT_TOOLTIP = "Add Comment";

    /** Creates a new instance with the given parameters. */
    public AddCommentButton(final ComponentEventListener<ClickEvent<Button>> addUserAction, final String tooltipText) {
        super("", addUserAction);
        this.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_WARNING);
        this.setIcon(LineAwesomeIcon.COMMENT_ALT_SOLID.create());
        this.setTooltipText(tooltipText != null ? tooltipText : DEFAULT_TOOLTIP);
    }

    public AddCommentButton(final ComponentEventListener<ClickEvent<Button>> addUserAction) {
        this(addUserAction, null);
    }
}
