package de.vptr.midas.gui.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;

import de.vptr.midas.gui.dto.UserResponseDto;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.UserGroupService;
import de.vptr.midas.gui.service.UserService;

class UserDetailViewTest extends AbstractUiTestBase {

    private UserDetailView view;

    @Mock
    UserService userService;

    @Mock
    UserGroupService userGroupService;

    @Mock
    AuthService authService;

    @BeforeEach
    void setUp() {
        this.view = new UserDetailView();
        this.view.userService = this.userService;
        this.view.userGroupService = this.userGroupService;
        this.view.authService = this.authService;
    }

    @Test
    void renderEntity_shouldShowChangePasswordButton_whenCurrentUser() {
        final var user = new UserResponseDto();
        user.setId(1L);
        user.setUsername("testuser");

        when(this.authService.getUsername()).thenReturn("testuser");
        when(this.userGroupService.getGroupsByUser(1L)).thenReturn(Collections.emptyList());

        this.view.renderEntity(user);

        final var buttons = this.view.getChildren()
                .filter(c -> c instanceof Button)
                .map(c -> (Button) c)
                .filter(b -> "Change Password".equals(b.getText()))
                .toList();

        assertThat(buttons).hasSize(1);
    }

    @Test
    void renderEntity_shouldNotShowChangePasswordButton_whenOtherUser() {
        final var user = new UserResponseDto();
        user.setId(1L);
        user.setUsername("testuser");

        when(this.authService.getUsername()).thenReturn("otheruser");
        when(this.userGroupService.getGroupsByUser(1L)).thenReturn(Collections.emptyList());

        this.view.renderEntity(user);

        final var buttons = this.view.getChildren()
                .filter(c -> c instanceof Button)
                .map(c -> (Button) c)
                .filter(b -> "Change Password".equals(b.getText()))
                .toList();

        assertThat(buttons).isEmpty();
    }
}
