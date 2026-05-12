package de.vptr.midas.gui.view;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import de.vptr.midas.gui.dto.UserRankResponseDto;

@ExtendWith(MockitoExtension.class)
class AdminMainLayoutTest extends AbstractMainLayoutTestBase {

    @InjectMocks
    AdminMainLayout adminMainLayout;

    @Override
    protected AbstractMainLayoutBase getLayout() {
        return this.adminMainLayout;
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void beforeEnter_shouldSkipChecks_whenTargetIsBackendErrorView() {
        when(this.event.getNavigationTarget()).thenReturn((Class) BackendErrorView.class);

        this.adminMainLayout.beforeEnter(this.event);

        verify(this.authService, never()).isAuthenticated();
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void beforeEnter_shouldForwardToLogin_whenNotAuthenticated() {
        when(this.event.getNavigationTarget()).thenReturn((Class) AdminHomeView.class);
        when(this.authService.isAuthenticated()).thenReturn(false);

        this.adminMainLayout.beforeEnter(this.event);

        verify(this.event).forwardTo(LoginView.class);
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void beforeEnter_shouldForwardToHome_whenNoAdminPermission() {
        when(this.event.getNavigationTarget()).thenReturn((Class) AdminHomeView.class);
        when(this.authService.isAuthenticated()).thenReturn(true);

        final var rank = new UserRankResponseDto();
        when(this.userRankService.getCurrentUserRank()).thenReturn(rank);

        this.adminMainLayout.beforeEnter(this.event);

        verify(this.event).forwardTo(HomeView.class);
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void beforeEnter_shouldAllowAccess_whenAdminPermissionGranted() {
        when(this.event.getNavigationTarget()).thenReturn((Class) AdminHomeView.class);
        when(this.authService.isAuthenticated()).thenReturn(true);

        final var rank = mock(UserRankResponseDto.class);
        when(rank.canAdminView()).thenReturn(true);
        when(this.userRankService.getCurrentUserRank()).thenReturn(rank);

        this.adminMainLayout.beforeEnter(this.event);

        verify(this.event, never()).forwardTo(any(Class.class));
        verify(this.event, never()).forwardTo(anyString());
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void beforeEnter_shouldSkipAuthCheck_whenTargetIsLoginView() {
        when(this.event.getNavigationTarget()).thenReturn((Class) LoginView.class);

        this.adminMainLayout.beforeEnter(this.event);

        verify(this.authService, never()).isAuthenticated();
    }
}
