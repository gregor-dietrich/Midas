package de.vptr.midas.gui.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableRunnable;

import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.service.AccountService;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.PageService;
import de.vptr.midas.gui.service.UserGroupService;

class AsyncAuthAlignmentViewTest {

    private UI ui;

    @BeforeEach
    void setUp() {
        this.ui = mock(UI.class);
        UI.setCurrent(this.ui);
        lenient().when(this.ui.accessLater(org.mockito.ArgumentMatchers.any(SerializableRunnable.class),
                org.mockito.ArgumentMatchers.any(SerializableRunnable.class)))
                .thenAnswer(invocation -> {
                    final var callback = invocation.getArgument(0, SerializableRunnable.class);
                    return (SerializableRunnable) () -> {
                        UI.setCurrent(this.ui);
                        try {
                            callback.run();
                        } finally {
                            UI.setCurrent(null);
                        }
                    };
                });
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
    }

    @Test
    void pageDetailView_shouldRouteAuthenticationFailureFromAsyncTitleLookup() throws Exception {
        final var authService = mock(AuthService.class);
        final var pageService = mock(PageService.class);
        final var view = new PageDetailView();
        final var latch = new CountDownLatch(1);

        view.authService = authService;
        view.pageService = pageService;
        when(authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(this.ui).navigate(LoginView.class);

        this.invoke(view, "loadPageByTitle", new Class<?>[] { String.class }, "missing-page");

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        verify(pageService, never()).searchPages(anyString(), anyString());
    }

    @Test
    void userDetailView_shouldRouteAuthenticationFailureFromAsyncGroupLookup() throws Exception {
        final var authService = mock(AuthService.class);
        final var userGroupService = mock(UserGroupService.class);
        final var view = new UserDetailView();
        final var latch = new CountDownLatch(1);

        view.authService = authService;
        view.userGroupService = userGroupService;
        when(authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(this.ui).navigate(LoginView.class);

        this.invoke(view, "loadGroupsAsync", new Class<?>[] { Long.class }, Long.valueOf(7));

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        verify(userGroupService, never()).getGroupsByUser(org.mockito.ArgumentMatchers.anyLong(), anyString());
    }

    @Test
    void accountView_shouldRouteAuthenticationFailureFromAsyncAccountLookup() throws Exception {
        final var authService = mock(AuthService.class);
        final var accountService = mock(AccountService.class);
        final var view = new AccountView();
        final var latch = new CountDownLatch(1);

        view.authService = authService;
        view.accountService = accountService;
        when(authService.requireAuthorizationHeader())
                .thenThrow(new AuthenticationException("Authentication required"));
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(this.ui).navigate(LoginView.class);

        this.invoke(view, "loadAccountsAsync", new Class<?>[0]);

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        verify(accountService, never()).getMyAccountsWithBalances(anyString());
    }

    private void invoke(final Object target, final String methodName, final Class<?>[] parameterTypes,
            final Object... arguments) throws Exception {
        final Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        method.invoke(target, arguments);
    }
}
