package de.vptr.midas.gui.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.vaadin.flow.component.UI;

import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.ErrorHandler;
import de.vptr.midas.gui.util.NotificationUtil;

class AccountPaymentViewTest {

    private MockedStatic<NotificationUtil> notificationMock;
    private UI ui;
    private AccountPaymentView view;

    @BeforeEach
    void setUp() {
        this.notificationMock = mockStatic(NotificationUtil.class);
        this.ui = mock(UI.class);
        this.view = new AccountPaymentView();
    }

    @AfterEach
    void tearDown() {
        this.notificationMock.close();
    }

    @Test
    void unwrapAsyncThrowable_shouldUnwrapNestedAsyncWrappers() {
        final var cause = new AuthenticationException("Session expired");
        final var wrapped = new CompletionException(new ExecutionException(cause));

        assertThat(ErrorHandler.unwrapAsyncThrowable(wrapped)).isSameAs(cause);
    }

    @Test
    void handleAccountAccessError_shouldNavigateToLogin_whenAuthenticationFails() {
        this.view.handleAccountAccessError(new CompletionException(new AuthenticationException("Session expired")),
                this.ui);

        this.notificationMock.verify(() -> NotificationUtil.showError("Session expired. Please log in again."));
        verify(this.ui).navigate(LoginView.class);
        verify(this.ui, never()).navigate(AccountView.class);
    }

    @Test
    void handleAccountAccessError_shouldNavigateToAccountView_whenAccessDenied() {
        this.view.handleAccountAccessError(new CompletionException(new ServiceException("Access denied")), this.ui);

        this.notificationMock.verify(() -> NotificationUtil.showError("Access denied or account not found"));
        verify(this.ui).navigate(AccountView.class);
        verify(this.ui, never()).navigate(LoginView.class);
    }

    @Test
    void handleAccountDataLoadError_shouldNavigateToLogin_whenAuthenticationFails() {
        this.view.handleAccountDataLoadError(new CompletionException(new AuthenticationException("Session expired")),
                this.ui);

        this.notificationMock.verify(() -> NotificationUtil.showError("Session expired. Please log in again."));
        verify(this.ui).navigate(LoginView.class);
    }

    @Test
    void handleAccountDataLoadError_shouldShowServiceMessage_whenServiceFails() {
        final var cause = new ServiceException("Backend error: 500");

        this.view.handleAccountDataLoadError(new CompletionException(cause), this.ui);

        this.notificationMock
                .verify(() -> NotificationUtil.showError(cause, "Loading account data failed. Please try again."));
        verify(this.ui, never()).navigate(LoginView.class);
    }

    @Test
    void handleAccountDataLoadError_shouldShowTimeoutMessage_whenRequestTimesOut() {
        this.view.handleAccountDataLoadError(new CompletionException(new TimeoutException("too slow")), this.ui);

        this.notificationMock
                .verify(() -> NotificationUtil.showError("Loading account data timed out. Please try again."));
        verify(this.ui, never()).navigate(LoginView.class);
    }
}
