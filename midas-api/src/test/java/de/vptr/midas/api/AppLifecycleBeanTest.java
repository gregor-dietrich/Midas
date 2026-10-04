package de.vptr.midas.api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import de.vptr.midas.api.service.UserService;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.StartupEvent;

class AppLifecycleBeanTest {

    private static final String PASSWORD = "B00tstr@p!";

    private final UserService userService = mock(UserService.class);

    private AppLifecycleBean bean(final LaunchMode launchMode, final String dbPassword, final boolean hasUsers,
            final Optional<String> password) {
        when(this.userService.hasUsers()).thenReturn(hasUsers);
        when(this.userService.createInitialAdmin("admin", PASSWORD)).thenReturn(true);
        return new AppLifecycleBean(launchMode, dbPassword, this.userService, "admin", password);
    }

    private AppLifecycleBean bean(final LaunchMode launchMode, final boolean hasUsers,
            final Optional<String> password) {
        return this.bean(launchMode, "not-the-default", hasUsers, password);
    }

    @Test
    void testProductionWithDefaultDbPasswordFails() {
        final var bean = this.bean(LaunchMode.NORMAL, "changeit", true, Optional.empty());
        assertThrows(IllegalStateException.class, () -> bean.onStart(new StartupEvent()));
        verify(this.userService, never()).hasUsers();
    }

    @Test
    void testProductionWithoutUsersAndWithoutPasswordFails() {
        final var bean = this.bean(LaunchMode.NORMAL, false, Optional.empty());
        assertThrows(IllegalStateException.class, () -> bean.onStart(new StartupEvent()));
        verify(this.userService, never()).createInitialAdmin(anyString(), anyString());
    }

    @Test
    void testProductionWithoutUsersCreatesAdmin() {
        final var bean = this.bean(LaunchMode.NORMAL, false, Optional.of(PASSWORD));
        assertDoesNotThrow(() -> bean.onStart(new StartupEvent()));
        verify(this.userService).createInitialAdmin("admin", PASSWORD);
    }

    @Test
    void testProductionWhenAdminNotCreatedStarts() {
        final var bean = this.bean(LaunchMode.NORMAL, false, Optional.of(PASSWORD));
        when(this.userService.createInitialAdmin("admin", PASSWORD)).thenReturn(false);
        assertDoesNotThrow(() -> bean.onStart(new StartupEvent()));
        verify(this.userService).createInitialAdmin("admin", PASSWORD);
    }

    @Test
    void testProductionWithUsersIgnoresPassword() {
        final var bean = this.bean(LaunchMode.NORMAL, true, Optional.of("weak"));
        assertDoesNotThrow(() -> bean.onStart(new StartupEvent()));
        verify(this.userService, never()).createInitialAdmin(anyString(), anyString());
    }

    @Test
    void testDevModeSkipsBootstrap() {
        final var bean = this.bean(LaunchMode.DEVELOPMENT, false, Optional.empty());
        assertDoesNotThrow(() -> bean.onStart(new StartupEvent()));
        verify(this.userService, never()).hasUsers();
    }
}
