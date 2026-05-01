package de.vptr.midas.gui.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableRunnable;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.server.ErrorEvent;
import com.vaadin.flow.server.VaadinSession;

class UiAsyncExecutorTest {

    @Test
    void dispatch_shouldRunUiCallback_whenUiAttached() {
        final var ui = mock(UI.class);
        final var executed = new AtomicBoolean(false);

        doAnswer(invocation -> {
            invocation.getArgument(0, Command.class).execute();
            return null;
        }).when(ui).access(any(Command.class));

        UiAsyncExecutor.dispatch(ui, null, () -> executed.set(true), null);

        assertThat(executed.get()).isTrue();
    }

    @Test
    void dispatch_shouldRouteDetachedErrorsToSessionHandler() {
        final var ui = mock(UI.class);
        final var session = mock(VaadinSession.class);
        final var sessionErrorHandler = mock(com.vaadin.flow.server.ErrorHandler.class);
        final var callbackExecuted = new AtomicBoolean(false);
        final var dispatchedError = new AtomicReference<ErrorEvent>();
        final var failure = new RuntimeException("boom");

        doThrow(new IllegalStateException("detached"))
                .when(ui)
                .access(any(Command.class));
        doAnswer(invocation -> {
            dispatchedError.set(invocation.getArgument(0));
            return null;
        }).when(sessionErrorHandler).error(any(ErrorEvent.class));
        doAnswer(invocation -> sessionErrorHandler).when(session).getErrorHandler();

        UiAsyncExecutor.dispatch(ui, session, () -> callbackExecuted.set(true), failure);

        assertThat(callbackExecuted).isFalse();
        assertThat(dispatchedError.get()).isNotNull();
        assertThat(dispatchedError.get().getThrowable()).isSameAs(failure);
        verify(sessionErrorHandler).error(any(ErrorEvent.class));
    }

    @Test
    void dispatch_shouldRouteDetachedErrors_whenUiMissing() {
        final var session = mock(VaadinSession.class);
        final var failure = new RuntimeException("boom");

        try (MockedStatic<ErrorHandler> errorHandlerMock = mockStatic(ErrorHandler.class)) {
            UiAsyncExecutor.dispatch(null, session, () -> {
            }, failure);

            errorHandlerMock.verify(() -> ErrorHandler.handleDetached(failure, session));
        }
    }

    @Test
    void dispatch_shouldRouteDetachedErrors_whenUiAccessFails() {
        final var ui = mock(UI.class);
        final var session = mock(VaadinSession.class);
        final var failure = new RuntimeException("boom");

        doThrow(new IllegalStateException("detached"))
                .when(ui)
                .access(any(Command.class));

        try (MockedStatic<ErrorHandler> errorHandlerMock = mockStatic(ErrorHandler.class)) {
            UiAsyncExecutor.dispatch(ui, session, () -> {
            }, failure);

            errorHandlerMock.verify(() -> ErrorHandler.handleDetached(failure, session));
        }
    }
}
