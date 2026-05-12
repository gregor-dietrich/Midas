package de.vptr.midas.gui.component.glass;

import com.vaadin.flow.component.html.Div;

/**
 * A decorative bar with a glass/gradient effect to be used at the top of layouts.
 */
public class GlassAccentBar extends Div {

    /**
     * Creates a new GlassAccentBar.
     */
    public GlassAccentBar() {
        this.setWidthFull();
        this.setHeight("3px");
        this.getStyle().set("position", "absolute");
        this.getStyle().set("top", "0");
        this.getStyle().set("left", "0");
        this.getStyle().set("background", "linear-gradient(to right, var(--lumo-primary-color), transparent)");
        this.getStyle().set("z-index", "10");
        this.getStyle().set("pointer-events", "none");
    }
}
