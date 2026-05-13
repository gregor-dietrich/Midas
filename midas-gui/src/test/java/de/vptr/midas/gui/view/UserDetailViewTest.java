package de.vptr.midas.gui.view;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import com.vaadin.flow.component.button.Button;

import de.vptr.midas.gui.dto.UserResponseDto;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.UserGroupService;
import de.vptr.midas.gui.service.UserService;

@SuppressWarnings("UnnecessaryAssignment")
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
        final var user = this.createTestUser();
        final var buttons = this.getChangePasswordButtons(user, "testuser");
        assertThat(buttons).hasSize(1);
    }

    @Test
    void renderEntity_shouldNotShowChangePasswordButton_whenOtherUser() {
        final var user = this.createTestUser();
        final var buttons = this.getChangePasswordButtons(user, "otheruser");
        assertThat(buttons).isEmpty();
    }

    private UserResponseDto createTestUser() {
        final var user = new UserResponseDto();
        user.setPublicId("1");
        user.setUsername("testuser");
        return user;
    }

    private List<Button> getChangePasswordButtons(final UserResponseDto user, final String currentUsername) {
        when(this.authService.getUsername()).thenReturn(currentUsername);
        lenient().when(this.userGroupService.getGroupsByUser("1")).thenReturn(Collections.emptyList());

        this.view.renderEntity(user);

        return this.view.getChildren().filter(Button.class::isInstance).map(c -> (Button) c)
                .filter(b -> "Change Password".equals(b.getText())).toList();
    }
}
