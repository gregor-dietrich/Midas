package de.vptr.midas.gui.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.VaadinSession;

import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.view.LoginView;

@ExtendWith(MockitoExtension.class)
class ErrorHandlerTest {

    private UI ui;
    private MockedStatic<UI> uiMock;
    private MockedStatic<NotificationUtil> notificationMock;

    @BeforeEach
    void setUp() {
        this.ui = mock(UI.class);
        // ui.access() runs the command immediately for testing
        lenient().doAnswer(invocation -> {
            final Command command = invocation.getArgument(0);
            command.execute();
            return null;
        }).when(this.ui).access(any(Command.class));

        this.uiMock = mockStatic(UI.class);
        this.uiMock.when(UI::getCurrent).thenReturn(this.ui);

        this.notificationMock = mockStatic(NotificationUtil.class);
    }

    @AfterEach
    void tearDown() {
        this.uiMock.close();
        this.notificationMock.close();
    }

    @Test
    void handle_shouldNavigateToLogin_whenAuthenticationException() {
        final var ex = new AuthenticationException("Session expired");

        ErrorHandler.handle(ex, this.ui);

        verify(this.ui).navigate(LoginView.class);
        this.notificationMock.verify(() -> NotificationUtil.showError("Session expired. Please log in again."));
    }

    @Test
    void handle_shouldShowError_whenServiceException() {
        final var ex = new ServiceException("Something went wrong");

        ErrorHandler.handle(ex, this.ui);

        this.notificationMock.verify(() -> NotificationUtil.showError("Operation failed. Please try again."));
        verify(this.ui, never()).navigate(LoginView.class);
    }

    @Test
    void handle_shouldShowGenericError_whenUnexpectedException() {
        final var ex = new RuntimeException("unexpected");

        ErrorHandler.handle(ex, this.ui);

        this.notificationMock.verify(() -> NotificationUtil.showError("An unexpected error occurred"));
        verify(this.ui, never()).navigate(LoginView.class);
    }

    @Test
    void handle_shouldShowTimeoutMessage_whenTimeoutException() {
        final var ex = new TimeoutException("slow");

        ErrorHandler.handle(ex, this.ui);

        this.notificationMock.verify(() -> NotificationUtil.showError("Operation timed out. Please try again."));
        verify(this.ui, never()).navigate(LoginView.class);
    }

    @Test
    void handle_shouldUnwrapCompletionException() {
        final var cause = new AuthenticationException("expired");
        final var wrapped = new CompletionException(cause);

        ErrorHandler.handle(wrapped, this.ui);

        verify(this.ui).navigate(LoginView.class);
    }

    @Test
    void handle_shouldUnwrapNestedCompletionException() {
        final var cause = new ServiceException("deep error");
        final var inner = new CompletionException(cause);
        final var outer = new CompletionException(inner);

        ErrorHandler.handle(outer, this.ui);

        this.notificationMock.verify(() -> NotificationUtil.showError("Operation failed. Please try again."));
    }

    @Test
    void handleInUiContext_shouldNavigateToLogin_whenAuthenticationException() {
        final var ex = new AuthenticationException("Session expired");

        ErrorHandler.handleInUiContext(ex);

        verify(this.ui).navigate(LoginView.class);
        this.notificationMock.verify(() -> NotificationUtil.showError("Session expired. Please log in again."));
    }

    @Test
    void handleInUiContext_shouldShowError_whenServiceException() {
        final var ex = new ServiceException("fail");

        ErrorHandler.handleInUiContext(ex);

        this.notificationMock.verify(() -> NotificationUtil.showError("Operation failed. Please try again."));
    }

    @Test
    void handleInUiContext_shouldShowGenericError_whenUnexpectedException() {
        final var ex = new NullPointerException("oops");

        ErrorHandler.handleInUiContext(ex);

        this.notificationMock.verify(() -> NotificationUtil.showError("An unexpected error occurred"));
    }

    @Test
    void handleInUiContext_shouldUnwrapCompletionException() {
        final var cause = new AuthenticationException("expired");
        final var wrapped = new CompletionException(cause);

        ErrorHandler.handleInUiContext(wrapped);

        verify(this.ui).navigate(LoginView.class);
    }

    @Test
    void handleDetached_shouldDispatchToSessionErrorHandler_whenSessionAvailable() {
        final var session = mock(VaadinSession.class);
        final var sessionErrorHandler = mock(com.vaadin.flow.server.ErrorHandler.class);
        final var ex = new RuntimeException("detached");

        lenient().when(session.getErrorHandler()).thenReturn(sessionErrorHandler);

        ErrorHandler.handleDetached(ex, session);

        verify(sessionErrorHandler).error(any(ErrorEvent.class));
    }

    @Test
    void handleDetached_shouldUnwrapCompletionExceptionBeforeDispatching() {
        final var session = mock(VaadinSession.class);
        final var sessionErrorHandler = mock(com.vaadin.flow.server.ErrorHandler.class);
        final var dispatchedEvent = new java.util.concurrent.atomic.AtomicReference<ErrorEvent>();
        final var rootCause = new ServiceException("detached");

        when(session.getErrorHandler()).thenReturn(sessionErrorHandler);
        doAnswer(invocation -> {
            dispatchedEvent.set(invocation.getArgument(0));
            return null;
        }).when(sessionErrorHandler).error(any(ErrorEvent.class));

        ErrorHandler.handleDetached(new CompletionException(rootCause), session);

        assertThat(dispatchedEvent.get()).isNotNull();
        assertThat(dispatchedEvent.get().getThrowable()).isSameAs(rootCause);
    }

    @Test
    void handleDetached_shouldNotPropagate_whenSessionErrorHandlerThrows() {
        final var session = mock(VaadinSession.class);
        final var sessionErrorHandler = mock(com.vaadin.flow.server.ErrorHandler.class);

        when(session.getErrorHandler()).thenReturn(sessionErrorHandler);
        doThrow(new IllegalStateException("handler failed")).when(sessionErrorHandler).error(any(ErrorEvent.class));

        assertThatCode(() -> ErrorHandler.handleDetached(new RuntimeException("detached"), session))
                .doesNotThrowAnyException();
        verify(sessionErrorHandler).error(any(ErrorEvent.class));
    }

    @Test
    void showSanitizedError_shouldUseFallbackMessage_whenUnexpectedExceptionContainsSensitiveText() {
        ErrorHandler.showSanitizedError(new RuntimeException("password=secret"),
                "Saving user failed. Please try again.");

        this.notificationMock.verify(() -> NotificationUtil.showError("Saving user failed. Please try again."));
    }

    @Test
    void showSanitizedError_shouldKeepAuthMessage_whenAuthenticationException() {
        ErrorHandler.showSanitizedError(new AuthenticationException("token expired"),
                "Saving user failed. Please try again.");

        this.notificationMock.verify(() -> NotificationUtil.showError("Session expired. Please log in again."));
    }
}
