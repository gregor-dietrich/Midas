package de.vptr.midas.gui.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.Command;

import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.service.AccountService;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.PageService;
import de.vptr.midas.gui.service.UserGroupService;

@SuppressWarnings("UnnecessaryAssignment")
class AsyncAuthAlignmentViewTest {

    private UI ui;

    @BeforeEach
    void setUp() {
        this.ui = mock(UI.class);
        UI.setCurrent(this.ui);
        lenient().when(this.ui.isAttached()).thenReturn(true);
        lenient().doAnswer(invocation -> {
            final var command = invocation.getArgument(0, Command.class);
            UI.setCurrent(this.ui);
            try {
                command.execute();
            } finally {
                UI.setCurrent(null);
            }
            return null;
        }).when(this.ui).access(ArgumentMatchers.any(Command.class));
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
        when(pageService.searchPages(anyString())).thenThrow(new AuthenticationException("Authentication required"));
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(this.ui).navigate(LoginView.class);

        this.invoke(view, "loadByName", new Class<?>[] { String.class }, "missing-page");

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void userDetailView_shouldRouteAuthenticationFailureFromAsyncGroupLookup() throws Exception {
        final var authService = mock(AuthService.class);
        final var userGroupService = mock(UserGroupService.class);
        final var view = new UserDetailView();
        final var latch = new CountDownLatch(1);

        view.authService = authService;
        view.userGroupService = userGroupService;
        when(userGroupService.getGroupsByUser(ArgumentMatchers.anyString()))
                .thenThrow(new AuthenticationException("Authentication required"));
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(this.ui).navigate(LoginView.class);

        this.invoke(view, "loadGroupsAsync", new Class<?>[] { String.class }, "7");

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void accountView_shouldRouteAuthenticationFailureFromAsyncAccountLookup() throws Exception {
        final var authService = mock(AuthService.class);
        final var accountService = mock(AccountService.class);
        final var view = new AccountView();
        final var latch = new CountDownLatch(1);

        view.authService = authService;
        view.accountService = accountService;
        when(accountService.getMyAccounts()).thenThrow(new AuthenticationException("Authentication required"));
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(this.ui).navigate(LoginView.class);

        this.invoke(view, "loadAccountsAsync", new Class<?>[0]);

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private void invoke(final Object target, final String methodName, final Class<?>[] parameterTypes,
            final Object... arguments) throws Exception {
        final Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        method.invoke(target, arguments);
    }
}
