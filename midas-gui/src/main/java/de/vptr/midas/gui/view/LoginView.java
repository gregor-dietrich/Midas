package de.vptr.midas.gui.view;

import java.util.concurrent.TimeoutException;

import org.jboss.logging.Logger;
import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.dto.AuthResultDto;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.util.MidasErrorHandler;
import de.vptr.midas.gui.util.NotificationUtil;
import de.vptr.midas.gui.util.UiAsyncExecutor;
import jakarta.inject.Inject;

/** Login view for user authentication. */
@Route(value = "login", layout = MainLayout.class)
@PageTitle("Midas - Login")
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger LOG = Logger.getLogger(LoginView.class);

    @Inject
    AuthService authService;

    private boolean initialized = false;

    /** Initializes LoginView. */
    public LoginView() {
        this.setSizeFull();
        this.setAlignItems(Alignment.CENTER);
        this.setJustifyContentMode(JustifyContentMode.CENTER);
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (this.authService.isAuthenticated()) {
            event.forwardTo(HomeView.class);
            return;
        }
        if (!this.initialized) {
            this.buildUi();
            this.initialized = true;
        }
    }

    private void buildUi() {
        final var title = new H1("Midas - Login");

        final var usernameField = new TextField("Username");
        usernameField.setPrefixComponent(LineAwesomeIcon.USER_SOLID.create());
        usernameField.setRequired(true);
        usernameField.setMaxLength(255);
        usernameField.setWidth("300px");

        final var passwordField = new PasswordField("Password");
        passwordField.setPrefixComponent(LineAwesomeIcon.LOCK_SOLID.create());
        passwordField.setRequired(true);
        passwordField.setMaxLength(255);
        passwordField.setWidth("300px");

        final var loginButton = new Button("Login");
        loginButton.addClickListener(e -> {
            final var username = usernameField.getValue();
            final var password = passwordField.getValue();

            try {
                // Disable button during authentication
                loginButton.setEnabled(false);
                loginButton.setText("Authenticating...");
                UiAsyncExecutor.execute(() -> this.authService.authenticate(username, password),
                        result -> this.handleAuthenticationResult(result, passwordField, loginButton),
                        throwable -> this.handleAuthenticationError(throwable, passwordField, loginButton));
            } catch (final RuntimeException ex) {
                LOG.error("Setup Exception during authentication", ex);
                this.resetLoginButton(loginButton);
            }
        });

        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.addClickShortcut(Key.ENTER);
        loginButton.setWidth("300px");

        this.add(title, usernameField, passwordField, loginButton);

        usernameField.focus();
    }

    private void handleAuthenticationResult(final AuthResultDto result, final PasswordField passwordField,
            final Button loginButton) {
        try {
            LOG.tracef("Authentication result - Status: %s, Message: %s", result.getStatus(), result.getMessage());

            switch (result.getStatus()) {
                case SUCCESS -> {
                    LOG.trace("Authentication successful, navigating to main view");
                    passwordField.clear();
                    this.getUI().ifPresent(ui -> ui.navigate(HomeView.class));
                }
                case INVALID_CREDENTIALS -> {
                    LOG.trace("Invalid credentials, showing error message");
                    NotificationUtil.showError(result.getMessage());
                    passwordField.clear();
                    passwordField.focus();
                }
                case BACKEND_UNAVAILABLE -> {
                    LOG.error("Backend unavailable during login, redirecting to error page");
                    this.getUI().ifPresent(ui -> ui.navigate(BackendErrorView.class));
                }
                case INVALID_INPUT -> {
                    LOG.trace("Invalid input, showing warning");
                    NotificationUtil.showWarning(result.getMessage());
                }
                default -> {
                    LOG.errorf("Unknown authentication result status: %s", result.getStatus());
                    NotificationUtil.showError("Unknown error occurred");
                }
            }
        } finally {
            this.resetLoginButton(loginButton);
        }
    }

    private void handleAuthenticationError(final Throwable throwable, final PasswordField passwordField,
            final Button loginButton) {
        try {
            final Throwable cause = MidasErrorHandler.unwrapAsyncThrowable(throwable);
            LOG.error("Exception during authentication", cause);
            if (cause instanceof TimeoutException) {
                NotificationUtil.showError("Authentication timed out. Please try again.");
            } else {
                NotificationUtil.showError(cause, "Login failed. Please try again.");
            }
            passwordField.clear();
            passwordField.focus();
        } finally {
            this.resetLoginButton(loginButton);
        }
    }

    private void resetLoginButton(final Button loginButton) {
        loginButton.setEnabled(true);
        loginButton.setText("Login");
    }
}
