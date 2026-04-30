package de.vptr.midas.gui.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;

import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.util.NotificationUtil;

class AdminPageViewTest {

    private MockedStatic<UI> uiMock;
    private MockedStatic<NotificationUtil> notificationMock;
    private UI ui;
    private AdminPageView view;
    private Button searchButton;

    @BeforeEach
    void setUp() throws Exception {
        this.ui = mock(UI.class);
        this.uiMock = mockStatic(UI.class);
        this.uiMock.when(UI::getCurrent).thenReturn(this.ui);
        this.notificationMock = mockStatic(NotificationUtil.class);

        this.view = new AdminPageView();
        this.searchButton = new Button("Searching...");
        this.searchButton.setEnabled(false);
        this.setField("searchButton", this.searchButton);
    }

    @AfterEach
    void tearDown() {
        this.uiMock.close();
        this.notificationMock.close();
    }

    @Test
    void handleSearchError_shouldResetButtonAndNavigateToLogin_whenAuthenticationFails() {
        this.view.handleSearchError(new AuthenticationException("Session expired"));

        assertThat(this.searchButton.isEnabled()).isTrue();
        assertThat(this.searchButton.getText()).isEqualTo("Search Pages");
        this.notificationMock.verify(() -> NotificationUtil.showError("Session expired. Please log in again."));
        verify(this.ui).navigate(LoginView.class);
    }

    private void setField(final String fieldName, final Object value) throws Exception {
        final Field field = AdminPageView.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(this.view, value);
    }
}
