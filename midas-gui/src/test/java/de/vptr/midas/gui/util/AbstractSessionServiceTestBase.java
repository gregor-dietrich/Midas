package de.vptr.midas.gui.util;

import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vaadin.flow.server.VaadinSession;

/** Base test class providing VaadinSession mock lifecycle management. */
@ExtendWith(MockitoExtension.class)
public abstract class AbstractSessionServiceTestBase {

    @Mock
    protected VaadinSession vaadinSession;
    protected MockedStatic<VaadinSession> vaadinSessionMock;

    @BeforeEach
    protected void setUpSession() {
        this.vaadinSessionMock = mockStatic(VaadinSession.class);
        this.vaadinSessionMock.when(VaadinSession::getCurrent).thenReturn(this.vaadinSession);
    }

    @AfterEach
    protected void tearDownSession() {
        this.vaadinSessionMock.close();
    }
}
