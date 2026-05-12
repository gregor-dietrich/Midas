package de.vptr.midas.gui.view;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MainLayoutTest extends AbstractMainLayoutTestBase {

    @InjectMocks
    MainLayout mainLayout;

    @Override
    protected AbstractMainLayoutBase getLayout() {
        return this.mainLayout;
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void beforeEnter_shouldSkipChecks_whenTargetIsBackendErrorView() {
        when(this.event.getNavigationTarget()).thenReturn((Class) BackendErrorView.class);

        this.mainLayout.beforeEnter(this.event);

        verify(this.authService, never()).isAuthenticated();
        verify(this.event, never()).forwardTo(any(Class.class));
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void beforeEnter_shouldSkipAuthCheck_whenTargetIsLoginView() {
        when(this.event.getNavigationTarget()).thenReturn((Class) LoginView.class);

        this.mainLayout.beforeEnter(this.event);

        verify(this.authService, never()).isAuthenticated();
        verify(this.event, never()).forwardTo(LoginView.class);
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void beforeEnter_shouldForwardToLogin_whenNotAuthenticated() {
        when(this.event.getNavigationTarget()).thenReturn((Class) HomeView.class);
        when(this.authService.isAuthenticated()).thenReturn(false);

        this.mainLayout.beforeEnter(this.event);

        verify(this.event).forwardTo(LoginView.class);
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void beforeEnter_shouldNotForward_whenAuthenticated() {
        when(this.event.getNavigationTarget()).thenReturn((Class) HomeView.class);
        when(this.authService.isAuthenticated()).thenReturn(true);

        this.mainLayout.beforeEnter(this.event);

        verify(this.event, never()).forwardTo(any(Class.class));
        verify(this.event, never()).forwardTo(anyString());
    }
}
