package de.vptr.midas.gui.view;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vaadin.flow.router.BeforeEnterEvent;

import de.vptr.midas.gui.dto.UserRankResponseDto;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.BackendAvailabilityService;
import de.vptr.midas.gui.service.ThemeService;
import de.vptr.midas.gui.service.UserRankService;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({ "unchecked", "rawtypes" })
class AdminMainLayoutTest {

    @Mock
    AuthService authService;

    @Mock
    BackendAvailabilityService backendAvailabilityService;

    @Mock
    ThemeService themeService;

    @Mock
    UserRankService userRankService;

    @InjectMocks
    AdminMainLayout adminMainLayout;

    @Mock
    BeforeEnterEvent event;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(this.themeService.getCurrentTheme()).thenReturn(ThemeService.Theme.LIGHT);
        // Skip Vaadin component initialization which requires VaadinService
        final Field initialized = AdminMainLayout.class.getDeclaredField("initialized");
        initialized.setAccessible(true);
        initialized.setBoolean(this.adminMainLayout, true);
    }

    @Test
    void beforeEnter_shouldSkipChecks_whenTargetIsBackendErrorView() {
        when(this.event.getNavigationTarget()).thenReturn((Class) BackendErrorView.class);

        this.adminMainLayout.beforeEnter(this.event);

        verify(this.backendAvailabilityService, never()).isBackendAvailable();
        verify(this.authService, never()).isAuthenticated();
    }

    @Test
    void beforeEnter_shouldForwardToError_whenBackendUnavailable() {
        when(this.event.getNavigationTarget()).thenReturn((Class) AdminHomeView.class);
        when(this.backendAvailabilityService.isBackendAvailable()).thenReturn(false);

        this.adminMainLayout.beforeEnter(this.event);

        verify(this.event).forwardTo(BackendErrorView.class);
    }

    @Test
    void beforeEnter_shouldForwardToLogin_whenNotAuthenticated() {
        when(this.event.getNavigationTarget()).thenReturn((Class) AdminHomeView.class);
        when(this.backendAvailabilityService.isBackendAvailable()).thenReturn(true);
        when(this.authService.isAuthenticated()).thenReturn(false);

        this.adminMainLayout.beforeEnter(this.event);

        verify(this.event).forwardTo(LoginView.class);
    }

    @Test
    void beforeEnter_shouldForwardToHome_whenNoAdminPermission() {
        when(this.event.getNavigationTarget()).thenReturn((Class) AdminHomeView.class);
        when(this.backendAvailabilityService.isBackendAvailable()).thenReturn(true);
        when(this.authService.isAuthenticated()).thenReturn(true);

        final var rank = new UserRankResponseDto();
        when(this.userRankService.getCurrentUserRank()).thenReturn(rank);

        this.adminMainLayout.beforeEnter(this.event);

        verify(this.event).forwardTo(HomeView.class);
    }

    @Test
    void beforeEnter_shouldAllowAccess_whenAdminPermissionGranted() {
        when(this.event.getNavigationTarget()).thenReturn((Class) AdminHomeView.class);
        when(this.backendAvailabilityService.isBackendAvailable()).thenReturn(true);
        when(this.authService.isAuthenticated()).thenReturn(true);

        final var rank = mock(UserRankResponseDto.class);
        when(rank.canAdminView()).thenReturn(true);
        when(this.userRankService.getCurrentUserRank()).thenReturn(rank);

        this.adminMainLayout.beforeEnter(this.event);

        verify(this.event, never()).forwardTo(any(Class.class));
        verify(this.event, never()).forwardTo(anyString());
    }

    @Test
    void beforeEnter_shouldSkipAuthCheck_whenTargetIsLoginView() {
        when(this.event.getNavigationTarget()).thenReturn((Class) LoginView.class);
        when(this.backendAvailabilityService.isBackendAvailable()).thenReturn(true);

        this.adminMainLayout.beforeEnter(this.event);

        verify(this.authService, never()).isAuthenticated();
    }
}
