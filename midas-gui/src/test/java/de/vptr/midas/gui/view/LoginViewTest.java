package de.vptr.midas.gui.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.lang.reflect.Method;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.PasswordField;

import de.vptr.midas.gui.util.NotificationUtil;

class LoginViewTest {

    private MockedStatic<NotificationUtil> notificationMock;

    @BeforeEach
    void setUp() {
        UI.setCurrent(mock(UI.class));
        this.notificationMock = mockStatic(NotificationUtil.class, CALLS_REAL_METHODS);
        this.notificationMock.when(() -> NotificationUtil.showError(anyString())).thenAnswer(invocation -> null);
    }

    @AfterEach
    void tearDown() {
        this.notificationMock.close();
        UI.setCurrent(null);
    }

    @Test
    void handleAuthenticationError_shouldHideRawExceptionText_whenUnexpectedFailureOccurs() throws Exception {
        final var view = new LoginView();
        final var passwordField = new PasswordField();
        passwordField.setValue("secret");
        final var loginButton = new Button("Authenticating...");
        loginButton.setEnabled(false);

        this.invoke(view, new RuntimeException("database password leaked"), passwordField, loginButton);

        this.notificationMock.verify(() -> NotificationUtil.showError("Login failed. Please try again."));
        assertThat(passwordField.getValue()).isEmpty();
        assertThat(loginButton.isEnabled()).isTrue();
        assertThat(loginButton.getText()).isEqualTo("Login");
    }

    @Test
    void handleAuthenticationError_shouldShowTimeoutMessage_whenAuthenticationTimesOut() throws Exception {
        final var view = new LoginView();
        final var passwordField = new PasswordField();
        final var loginButton = new Button("Authenticating...");
        loginButton.setEnabled(false);

        this.invoke(view, new TimeoutException("slow backend"), passwordField, loginButton);

        this.notificationMock.verify(() -> NotificationUtil.showError("Authentication timed out. Please try again."));
        assertThat(loginButton.isEnabled()).isTrue();
        assertThat(loginButton.getText()).isEqualTo("Login");
    }

    private void invoke(final LoginView view, final Throwable throwable, final PasswordField passwordField,
            final Button loginButton) throws Exception {
        final Method method = LoginView.class.getDeclaredMethod("handleAuthenticationError", Throwable.class,
                PasswordField.class, Button.class);
        method.setAccessible(true);
        method.invoke(view, throwable, passwordField, loginButton);
    }
}
