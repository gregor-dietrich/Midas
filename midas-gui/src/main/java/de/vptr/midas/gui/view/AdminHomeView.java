package de.vptr.midas.gui.view;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.service.AuthService;
import jakarta.inject.Inject;

/** Admin home view displaying welcome message. */
@Route(value = "admin", layout = AdminMainLayout.class)
public class AdminHomeView extends VerticalLayout implements BeforeEnterObserver {

    @Inject
    transient AuthService authService;

    public AdminHomeView() {
        this.setAlignItems(Alignment.START);
        this.setJustifyContentMode(JustifyContentMode.START);
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (!this.authService.isAuthenticated()) {
            event.forwardTo(LoginView.class);
            return;
        }
        this.buildUi();
    }

    private void buildUi() {
        this.removeAll();
        final var username = this.authService.getUsername();
        final var welcomeLabel = new H2("Welcome, " + (username != null ? username : "Guest") + "!");
        this.add(welcomeLabel);
    }
}
