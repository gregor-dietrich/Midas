package de.vptr.midas.gui.view;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;

import de.vptr.midas.gui.service.AuthService;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** Base view for views that accept a string URL parameter and require user authentication. */
public abstract class AbstractAuthenticatedParameterView extends AbstractAsyncDataView
        implements BeforeEnterObserver, HasUrlParameter<String> {

    @Inject
    protected transient AuthService authService;

    @Nullable
    protected String identifier;

    protected AbstractAuthenticatedParameterView() {
        this.setSizeFull();
        this.setPadding(true);
        this.setSpacing(true);
    }

    @Override
    public void setParameter(final BeforeEvent event, final String parameter) {
        this.identifier = parameter;
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (!this.authService.isAuthenticated()) {
            event.forwardTo(LoginView.class);
            return;
        }
        this.onBeforeEnterAuthenticated(event);
    }

    /** Called during beforeEnter after successful authentication. */
    protected abstract void onBeforeEnterAuthenticated(BeforeEnterEvent event);
}
