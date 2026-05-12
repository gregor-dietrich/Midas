package de.vptr.midas.gui.view;

import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;

import com.vaadin.flow.router.BeforeEnterEvent;

import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.ThemeService;
import de.vptr.midas.gui.service.UserRankService;

/** Base class for main layout tests to avoid code duplication. */
public abstract class AbstractMainLayoutTestBase {

    @Mock
    protected AuthService authService;

    @Mock
    protected ThemeService themeService;

    @Mock
    protected UserRankService userRankService;

    @Mock
    protected BeforeEnterEvent event;

    protected abstract AbstractMainLayoutBase getLayout();

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    @BeforeEach
    void setUp() throws Exception {
        lenient().when(this.themeService.getCurrentTheme()).thenReturn(ThemeService.Theme.LIGHT);
        // Skip Vaadin component initialization which requires VaadinService
        final Field initialized = AbstractMainLayoutBase.class.getDeclaredField("initialized");
        initialized.setAccessible(true);
        initialized.setBoolean(this.getLayout(), true);
    }
}
