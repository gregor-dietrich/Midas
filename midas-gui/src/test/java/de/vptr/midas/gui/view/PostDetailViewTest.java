package de.vptr.midas.gui.view;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.vaadin.flow.component.UI;

import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.util.NotificationUtil;

class PostDetailViewTest {

    private MockedStatic<UI> uiMock;
    private MockedStatic<NotificationUtil> notificationMock;
    private UI ui;
    private PostDetailView view;

    @BeforeEach
    void setUp() {
        this.ui = mock(UI.class);
        this.uiMock = mockStatic(UI.class);
        this.uiMock.when(UI::getCurrent).thenReturn(this.ui);
        this.notificationMock = mockStatic(NotificationUtil.class);
        this.view = new PostDetailView();
    }

    @AfterEach
    void tearDown() {
        this.uiMock.close();
        this.notificationMock.close();
    }

    @Test
    void handleCommentLoadError_shouldNavigateToLogin_whenAuthenticationFails() {
        this.view.handleCommentLoadError(new AuthenticationException("Session expired"));

        this.notificationMock.verify(() -> NotificationUtil.showError("Session expired. Please log in again."));
        verify(this.ui).navigate(LoginView.class);
    }
}
