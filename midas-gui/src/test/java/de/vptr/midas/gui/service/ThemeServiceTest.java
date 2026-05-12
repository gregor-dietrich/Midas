package de.vptr.midas.gui.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ThemeList;
import com.vaadin.flow.server.VaadinSession;

import de.vptr.midas.gui.service.ThemeService.Theme;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.LinguisticNaming")
class ThemeServiceTest {

    @InjectMocks
    ThemeService themeService;

    private VaadinSession vaadinSession;
    private UI ui;
    private Element element;
    private ThemeList themeList;
    private Page page;

    private MockedStatic<VaadinSession> vaadinSessionMock;
    private MockedStatic<UI> uiMock;

    @BeforeEach
    void setUp() {
        // Create fresh mocks for each test
        this.vaadinSession = Mockito.mock(VaadinSession.class);
        this.ui = Mockito.mock(UI.class);
        this.element = Mockito.mock(Element.class);
        this.themeList = Mockito.mock(ThemeList.class);
        this.page = Mockito.mock(Page.class);

        this.vaadinSessionMock = mockStatic(VaadinSession.class);
        this.uiMock = mockStatic(UI.class);

        this.vaadinSessionMock.when(VaadinSession::getCurrent).thenReturn(this.vaadinSession);
        this.uiMock.when(UI::getCurrent).thenReturn(this.ui);

        // Use lenient() to avoid unnecessary stubbing errors for tests that don't need
        // these
        lenient().when(this.ui.getElement()).thenReturn(this.element);
        lenient().when(this.element.getThemeList()).thenReturn(this.themeList);
        lenient().when(this.ui.getPage()).thenReturn(this.page);
    }

    @AfterEach
    void tearDown() {
        this.vaadinSessionMock.close();
        this.uiMock.close();
    }

    @Test
    void getCurrentTheme_shouldReturnSystem_whenNoThemeInSession() {
        // Given
        when(this.vaadinSession.getAttribute("selected_theme")).thenReturn(null);

        // When
        final Theme result = this.themeService.getCurrentTheme();

        // Then
        assertThat(result).isEqualTo(Theme.SYSTEM);
    }

    @Test
    void getCurrentTheme_shouldReturnStoredTheme_whenThemeInSession() {
        // Given
        when(this.vaadinSession.getAttribute("selected_theme")).thenReturn(Theme.DARK);

        // When
        final Theme result = this.themeService.getCurrentTheme();

        // Then
        assertThat(result).isEqualTo(Theme.DARK);
    }

    @Test
    void getCurrentTheme_shouldReturnSystem_whenSessionIsNull() {
        // Given
        this.vaadinSessionMock.when(VaadinSession::getCurrent).thenReturn(null);

        // When
        final Theme result = this.themeService.getCurrentTheme();

        // Then
        assertThat(result).isEqualTo(Theme.SYSTEM);
    }

    @Test
    void setTheme_shouldStoreThemeInSession() {
        // When
        this.themeService.setTheme(Theme.DARK);

        // Then
        verify(this.vaadinSession).setAttribute("selected_theme", Theme.DARK);
    }

    @Test
    void applyTheme_shouldApplyDarkTheme() {
        // When
        this.themeService.applyTheme(Theme.DARK);

        // Then
        verify(this.page).executeJs("document.documentElement.removeAttribute('theme');");
        verify(this.themeList).clear();
        verify(this.themeList).add("dark");
    }

    @Test
    void applyTheme_shouldApplyLightTheme() {
        // When
        this.themeService.applyTheme(Theme.LIGHT);

        // Then
        verify(this.page).executeJs("document.documentElement.removeAttribute('theme');");
        verify(this.themeList).clear();
    }

    @Test
    void applyTheme_shouldApplySystemTheme() {
        // When
        this.themeService.applyTheme(Theme.SYSTEM);

        // Then
        verify(this.themeList).clear();
        verify(this.page).executeJs(anyString());
    }

    @Test
    void getNextTheme_shouldCycleFromLightToDark() {
        // Given
        when(this.vaadinSession.getAttribute("selected_theme")).thenReturn(Theme.LIGHT);

        // When
        final Theme result = this.themeService.getNextTheme();

        // Then
        assertThat(result).isEqualTo(Theme.DARK);
    }

    @Test
    void getNextTheme_shouldCycleFromDarkToSystem() {
        // Given
        when(this.vaadinSession.getAttribute("selected_theme")).thenReturn(Theme.DARK);

        // When
        final Theme result = this.themeService.getNextTheme();

        // Then
        assertThat(result).isEqualTo(Theme.SYSTEM);
    }

    @Test
    void getNextTheme_shouldCycleFromSystemToLight() {
        // Given
        when(this.vaadinSession.getAttribute("selected_theme")).thenReturn(Theme.SYSTEM);

        // When
        final Theme result = this.themeService.getNextTheme();

        // Then
        assertThat(result).isEqualTo(Theme.LIGHT);
    }

    @Test
    void themeEnum_shouldHaveCorrectDisplayNames() {
        assertThat(Theme.LIGHT.getDisplayName()).isEqualTo("Light");
        assertThat(Theme.DARK.getDisplayName()).isEqualTo("Dark");
        assertThat(Theme.SYSTEM.getDisplayName()).isEqualTo("System");
    }

    @Test
    void themeEnum_shouldHaveCorrectThemeVariants() {
        assertThat(Theme.LIGHT.getThemeVariant()).isNull();
        assertThat(Theme.DARK.getThemeVariant()).isEqualTo("dark");
        assertThat(Theme.SYSTEM.getThemeVariant()).isNull();
    }
}
