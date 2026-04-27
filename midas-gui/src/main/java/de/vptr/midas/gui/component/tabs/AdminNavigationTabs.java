package de.vptr.midas.gui.component.tabs;

import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;

import de.vptr.midas.gui.dto.UserRankResponseDto;
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
    public AdminNavigationTabs(final UserRankResponseDto userRank) {
        this.setOrientation(Orientation.VERTICAL);
        this.setWidthFull();
        this.add(new Tab(new RouterLink("Home", AdminHomeView.class)));

        if (userRank.hasAnyUserAccountPermission()) {
            this.add(new Tab(new RouterLink("Accounts", AdminAccountView.class)));
            this.add(new Tab(new RouterLink("Payments", AdminPaymentView.class)));
        }

        if (userRank.hasAnyPagePermission()) {
            this.add(new Tab(new RouterLink("Pages", AdminPageView.class)));
        }

        if (userRank.hasAnyPostPermission()) {
            this.add(new Tab(new RouterLink("Posts", AdminPostView.class)));
        }

        if (userRank.hasAnyPostCategoryPermission()) {
            this.add(new Tab(new RouterLink("Post Categories", AdminPostCategoryView.class)));
        }

        if (userRank.hasAnyPostCommentPermission()) {
            this.add(new Tab(new RouterLink("Post Comments", AdminPostCommentView.class)));
        }

        if (userRank.hasAnyUserPermission()) {
            this.add(new Tab(new RouterLink("Users", AdminUserView.class)));
        }

        if (userRank.hasAnyUserGroupPermission()) {
            this.add(new Tab(new RouterLink("User Groups", AdminUserGroupView.class)));
        }

        if (userRank.hasAnyUserRankPermission()) {
            this.add(new Tab(new RouterLink("User Ranks", AdminUserRankView.class)));
        }
    }
}
