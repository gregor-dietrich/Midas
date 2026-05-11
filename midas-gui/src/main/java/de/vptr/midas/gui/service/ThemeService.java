package de.vptr.midas.gui.service;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.Lumo;

import jakarta.enterprise.context.ApplicationScoped;

/** Component. */
@ApplicationScoped
public class ThemeService {

    /** Theme component. */
    @SuppressWarnings("NullAway")
    public enum Theme {
        LIGHT("Light", null), DARK("Dark", Lumo.DARK), SYSTEM("System", null);

        private final String displayName;
        private final String themeVariant;

        @SuppressWarnings("NullAway")
        Theme(final String displayName, final String themeVariant) {
            this.displayName = displayName;
            this.themeVariant = themeVariant;
        }

        /**
         * Returns the display name of this theme.
         *
         * @return the display name
         */
        public String getDisplayName() {
            return this.displayName;
        }

        /**
         * Returns the theme variant string used by Vaadin.
         *
         * @return the theme variant, or {@code null} for default
         */
        public String getThemeVariant() {
            return this.themeVariant;
        }
    }

    private static final String THEME_SESSION_KEY = "selected_theme";

    /** Returns the currently active theme. */
    public Theme getCurrentTheme() {
        final var session = VaadinSession.getCurrent();
        if (session != null) {
            final var theme = (Theme) session.getAttribute(THEME_SESSION_KEY);
            return theme != null ? theme : Theme.SYSTEM;
        }
        return Theme.SYSTEM;
    }

    /** Sets the active theme. */
    public void setTheme(final Theme theme) {
        final var session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(THEME_SESSION_KEY, theme);
        }
        this.applyTheme(theme);
    }

    /** Applies the given theme to the UI. */
    public void applyTheme(final Theme theme) {
        final var ui = UI.getCurrent();
        if (ui != null) {
            final var themeList = ui.getElement().getThemeList();

            switch (theme) {
                case DARK -> {
                    ui.getPage().executeJs("document.documentElement.removeAttribute('theme');");
                    themeList.clear();
                    themeList.add(Lumo.DARK);
                }
                case LIGHT -> {
                    ui.getPage().executeJs("document.documentElement.removeAttribute('theme');");
                    themeList.clear();
                }
                case SYSTEM -> {
                    themeList.clear();
                    ui.getPage().executeJs(
                            "if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {"
                                    + "  document.documentElement.setAttribute('theme', 'dark');" + "} else {"
                                    + "  document.documentElement.removeAttribute('theme');" + "}");
                }
                default -> {}
            }
        }
    }

    /** Returns the next theme in rotation. */
    public Theme getNextTheme() {
        final var current = this.getCurrentTheme();
        return switch (current) {
            case LIGHT -> Theme.DARK;
            case DARK -> Theme.SYSTEM;
            case SYSTEM -> Theme.LIGHT;
        };
    }
}
