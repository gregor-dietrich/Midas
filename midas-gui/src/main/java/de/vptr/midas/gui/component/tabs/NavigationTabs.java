package de.vptr.midas.gui.component.tabs;

import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;

import de.vptr.midas.gui.view.AccountView;
import de.vptr.midas.gui.view.HomeView;
import de.vptr.midas.gui.view.PageView;
import de.vptr.midas.gui.view.PostCategoryView;
import de.vptr.midas.gui.view.PostView;
import de.vptr.midas.gui.view.UserGroupView;
import de.vptr.midas.gui.view.UserView;

/** Navigation tabs for navigation. */
public class NavigationTabs extends Tabs {

    /** Initializes NavigationTabs. */
    public NavigationTabs() {
        this.add(new Tab(new RouterLink("Home", HomeView.class)));
        this.add(new Tab(new RouterLink("Accounts", AccountView.class)));
        this.add(new Tab(new RouterLink("Posts", PostView.class)));
        this.add(new Tab(new RouterLink("Categories", PostCategoryView.class)));
        this.add(new Tab(new RouterLink("Users", UserView.class)));
        this.add(new Tab(new RouterLink("Groups", UserGroupView.class)));
        this.add(new Tab(new RouterLink("Pages", PageView.class)));
    }
}
