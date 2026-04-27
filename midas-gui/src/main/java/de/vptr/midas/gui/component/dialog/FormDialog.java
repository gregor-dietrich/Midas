package de.vptr.midas.gui.component.dialog;

import com.vaadin.flow.component.dialog.Dialog;

/** Dialog for form. */
public class FormDialog extends Dialog {

    private static final String DEFAULT_WIDTH = "800px";

    public FormDialog() {
        this(DEFAULT_WIDTH);
    }

    /** Creates a new instance with the given parameters. */
    public FormDialog(final String width) {
        this.setWidth(width);
        this.setCloseOnEsc(true);
        this.setCloseOnOutsideClick(false);
    }

    public FormDialog(final String width, final String height) {
        this(width);
        this.setHeight(height);
    }
}
