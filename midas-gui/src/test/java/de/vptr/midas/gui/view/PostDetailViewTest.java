package de.vptr.midas.gui.view;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.server.Command;
import com.vaadin.flow.shared.Registration;

import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.service.Broadcaster;
import de.vptr.midas.gui.service.PostCommentService;
import de.vptr.midas.gui.util.NotificationUtil;

@SuppressWarnings({ "PMD.AvoidAccessibilityAlteration", "PMD.AvoidThrowingRawExceptionTypes", "StringConcatToTextBlock",
        "UnnecessaryAssignment" })
class PostDetailViewTest {

    private MockedStatic<UI> uiMock;
    private MockedStatic<NotificationUtil> notificationMock;
    private UI ui;
    private PostDetailView view;
    private Broadcaster broadcaster;
    private PostCommentService postCommentService;

    @BeforeEach
    void setUp() throws Exception {
        this.ui = mock(UI.class);
        this.uiMock = mockStatic(UI.class);
        this.uiMock.when(UI::getCurrent).thenReturn(this.ui);
        this.notificationMock = mockStatic(NotificationUtil.class);
        this.broadcaster = mock(Broadcaster.class);
        this.postCommentService = mock(PostCommentService.class);
        this.view = new PostDetailView();
        this.view.broadcaster = this.broadcaster;
        this.view.postCommentService = this.postCommentService;

        // Manually trigger component internal state to allow getUI() to work
        final var getElementMethod = Component.class.getDeclaredMethod("getElement");
        getElementMethod.setAccessible(true);
        final var element = ElementFactory.createDiv();
        final var field = Component.class.getDeclaredField("element");
        field.setAccessible(true);
        field.set(this.view, element);

        // Mock getUI() on the component
        this.view = Mockito.spy(this.view);
        Mockito.doReturn(Optional.of(this.ui)).when(this.view).getUI();
    }

    @AfterEach
    void tearDown() {
        this.uiMock.close();
        this.notificationMock.close();
    }

    @Test
    void onAttach_shouldRegisterBroadcaster() {
        final var registration = mock(Registration.class);
        when(this.broadcaster.register(any())).thenReturn(registration);

        this.view.onAttach(new AttachEvent(this.view, true));

        verify(this.broadcaster).register(any());
    }

    @Test
    void onDetach_shouldUnregisterBroadcaster() {
        final var registration = mock(Registration.class);
        when(this.broadcaster.register(any())).thenReturn(registration);

        this.view.onAttach(new AttachEvent(this.view, true));

        this.view.onDetach(new DetachEvent(this.view));

        verify(registration).remove();
    }

    @Test
    @SuppressWarnings("unchecked")
    void broadcasterCallback_shouldRefreshComments_whenMessageMatchesCurrentPost() {
        final ArgumentCaptor<Consumer<String>> captor = ArgumentCaptor.forClass(Consumer.class);
        when(this.broadcaster.register(captor.capture())).thenReturn(mock(Registration.class));

        try {
            final var field = PostDetailView.class.getDeclaredField("currentPostId");
            field.setAccessible(true);
            field.set(this.view, "123");
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        this.view.onAttach(new AttachEvent(this.view, true));

        final var callback = captor.getValue();

        // Mock UI.access to run immediately
        when(this.ui.access(any(Command.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, Command.class).execute();
            // UI.access returns a Registration that might be cast to Future.
            return mock(Registration.class, Mockito.withSettings().extraInterfaces(Future.class));
        });

        callback.accept("comment-created:123");

        verify(this.postCommentService).getCommentsByPost("123");
    }

    @Test
    void handleCommentLoadError_shouldNavigateToLogin_whenAuthenticationFails() {
        this.view.handleCommentLoadError(new AuthenticationException("Session expired"));

        this.notificationMock.verify(() -> NotificationUtil.showError("Session expired. Please log in again."));
        verify(this.ui).navigate(LoginView.class);
    }
}
