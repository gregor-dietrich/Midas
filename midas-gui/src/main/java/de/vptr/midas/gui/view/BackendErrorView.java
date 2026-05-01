package de.vptr.midas.gui.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.service.BackendAvailabilityService;
import de.vptr.midas.gui.service.ThemeService;
import de.vptr.midas.gui.util.NotificationUtil;
import de.vptr.midas.gui.util.UiAsyncExecutor;
import jakarta.inject.Inject;

/** View displayed when the backend is unavailable. */
@Route(value = "backend-error", layout = MainLayout.class)
@PageTitle("Midas - Backend Unavailable")
public class BackendErrorView extends VerticalLayout implements BeforeEnterObserver {

    @Inject
    ThemeService themeService;

    @Inject
    BackendAvailabilityService backendAvailabilityService;

    private boolean initialized = false;

    /** Initializes BackendErrorView. */
    public BackendErrorView() {
        this.setSizeFull();
        this.setAlignItems(Alignment.CENTER);
        this.setJustifyContentMode(JustifyContentMode.CENTER);
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (!this.initialized) {
            this.buildUi();
            this.initialized = true;
        }
    }

    private void buildUi() {
        final var errorIcon = new Icon(VaadinIcon.EXCLAMATION_CIRCLE);
        errorIcon.setSize("64px");
        errorIcon.setColor("var(--lumo-error-color)");

        final var title = new H1("Backend unavailable");
        title.getStyle().set("color", "var(--lumo-error-color)");

        final var subtitle = new H3("Midas cannot reach backend right now. 🙁");

        final var explanation = new Paragraph(
                "Please try again in a moment. If issue persists, check your connection or contact support.");
        explanation.getStyle().set("text-align", "center");
        explanation.getStyle().set("max-width", "600px");

        final var buttonLayout = new HorizontalLayout();

        final var retryButton = new Button("Retry", e -> {
            UiAsyncExecutor.execute(
                    () -> this.backendAvailabilityService.isBackendAvailableRaw(),
                    available -> {
                        if (Boolean.TRUE.equals(available)) {
                            this.getUI().ifPresent(ui -> ui.navigate(HomeView.class));
                        } else {
                            NotificationUtil.showError("Backend is still unavailable. Please try again later.");
                        }
                    },
                    throwable -> NotificationUtil.showError("Failed to check backend status. Please try again."));
        });
        retryButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var retryIcon = new Icon(VaadinIcon.REFRESH);
        retryButton.setIcon(retryIcon);

        buttonLayout.add(retryButton);

        this.add(errorIcon, title, subtitle, explanation, buttonLayout);
        this.setSpacing(true);
        this.setPadding(true);
    }
}
