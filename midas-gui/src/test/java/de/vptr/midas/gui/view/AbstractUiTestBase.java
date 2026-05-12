package de.vptr.midas.gui.view;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.Command;

import de.vptr.midas.gui.util.NotificationUtil;

/** Base class for tests requiring UI and NotificationUtil mocking. */
@ExtendWith(MockitoExtension.class)
public abstract class AbstractUiTestBase {

    protected UI ui;
    protected MockedStatic<UI> uiMock;
    protected MockedStatic<NotificationUtil> notificationMock;

    @BeforeEach
    protected void setUpUiMock() {
        this.ui = mock(UI.class);
        lenient().when(this.ui.isAttached()).thenReturn(true);
        lenient().doAnswer(invocation -> {
            invocation.getArgument(0, Command.class).execute();
            return null;
        }).when(this.ui).access(any(Command.class));

        this.uiMock = mockStatic(UI.class);
        this.uiMock.when(UI::getCurrent).thenReturn(this.ui);

        this.notificationMock = mockStatic(NotificationUtil.class);
    }

    @AfterEach
    protected void tearDownUiMock() {
        if (this.uiMock != null) {
            this.uiMock.close();
        }
        if (this.notificationMock != null) {
            this.notificationMock.close();
        }
    }
}
