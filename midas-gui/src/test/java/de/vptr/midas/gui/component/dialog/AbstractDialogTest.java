package de.vptr.midas.gui.component.dialog;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockedStatic;

import com.vaadin.flow.component.UI;

import de.vptr.midas.gui.util.NotificationUtil;

/** Base class for dialog tests providing common setup and mocks. */
@SuppressWarnings("NullAway")
public abstract class AbstractDialogTest {

    protected UI ui;
    protected Runnable onSave;
    protected Runnable onAuthFailure;
    protected MockedStatic<NotificationUtil> notificationMock;

    @BeforeEach
    void setUpBase() {
        this.ui = new UI();
        UI.setCurrent(this.ui);
        this.onSave = mock(Runnable.class);
        this.onAuthFailure = mock(Runnable.class);
        this.notificationMock = mockStatic(NotificationUtil.class);
    }

    @AfterEach
    void tearDownBase() {
        UI.setCurrent(null);
        if (this.notificationMock != null) {
            this.notificationMock.close();
        }
    }
}
