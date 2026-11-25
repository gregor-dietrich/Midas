package de.vptr.midas.gui.component.tabs;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;

import de.vptr.midas.gui.dto.UserRankDto;
import de.vptr.midas.gui.view.AdminAccountView;
import de.vptr.midas.gui.view.AdminHomeView;
import de.vptr.midas.gui.view.AdminPageView;
import de.vptr.midas.gui.view.AdminPaymentView;
import de.vptr.midas.gui.view.AdminPostCategoryView;
import de.vptr.midas.gui.view.AdminPostCommentView;
import de.vptr.midas.gui.view.AdminPostView;
import de.vptr.midas.gui.view.AdminUserGroupView;
import de.vptr.midas.gui.view.AdminUserRankView;
import de.vptr.midas.gui.view.AdminUserView;

/** Navigation tabs for admin navigation. */
public class AdminNavigationTabs extends Tabs {

    /** Creates a new instance with the given parameters. */
    public AdminNavigationTabs(final UserRankDto userRank) {
        this.setOrientation(Orientation.VERTICAL);
        this.setWidthFull();
        this.add(new Tab(new RouterLink("Home", AdminHomeView.class)));

        if (userRank.hasAnyUserAccountPermission()) {
            this.add(new Tab(new RouterLink("Accounts", AdminAccountView.class)));
            this.add(new Tab(new RouterLink("Payments", AdminPaymentView.class)));
        }

        this.addTabIf(userRank.hasAnyPagePermission(), "Pages", AdminPageView.class);
        this.addTabIf(userRank.hasAnyPostPermission(), "Posts", AdminPostView.class);
        this.addTabIf(userRank.hasAnyPostCategoryPermission(), "Post Categories", AdminPostCategoryView.class);
        this.addTabIf(userRank.hasAnyPostCommentPermission(), "Post Comments", AdminPostCommentView.class);
        this.addTabIf(userRank.hasAnyUserPermission(), "Users", AdminUserView.class);
        this.addTabIf(userRank.hasAnyUserGroupPermission(), "User Groups", AdminUserGroupView.class);
        this.addTabIf(userRank.hasAnyUserRankPermission(), "User Ranks", AdminUserRankView.class);
    }

    private void addTabIf(final boolean condition, final String label, final Class<? extends Component> target) {
        if (condition) {
            this.add(new Tab(new RouterLink(label, target)));
        }
    }
}
