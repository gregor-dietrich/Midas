package de.vptr.midas.gui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.Theme;

/** Component. */
@SuppressWarnings("deprecation")
@Theme("starter-theme")
@PageTitle("Midas")
@Push
public class AppConfig implements AppShellConfigurator {
}
