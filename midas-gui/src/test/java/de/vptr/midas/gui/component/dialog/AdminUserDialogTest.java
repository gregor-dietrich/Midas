package de.vptr.midas.gui.component.dialog;

import static de.vptr.midas.gui.util.UiTestUtil.findButton;
import static de.vptr.midas.gui.util.UiTestUtil.findComponent;
import static de.vptr.midas.gui.util.UiTestUtil.findComponents;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;

import de.vptr.midas.gui.dto.UserDto;
import de.vptr.midas.gui.dto.UserRankResponseDto;
import de.vptr.midas.gui.dto.UserResponseDto;
import de.vptr.midas.gui.service.UserService;
import de.vptr.midas.gui.util.MockDataUtil;
import de.vptr.midas.gui.util.NotificationUtil;

@ExtendWith(MockitoExtension.class)
class AdminUserDialogTest extends AbstractDialogTest {

    @Mock
    UserService userService;

    @Test
    void saveUser_shouldPreserveEnteredPassword_onCreate() {
        final var dialog = new AdminUserDialog(this.userService, this.onSave, this.onAuthFailure);
        final var rank = this.createRank("1", "Administrator");
        when(this.userService.createUser(any(UserDto.class)))
                .thenReturn(MockDataUtil.createUserResponseDto("1", "newuser", "new@example.com"));

        dialog.open(null, List.of(rank));

        findComponent(dialog, TextField.class, field -> "Username".equals(field.getLabel())).setValue("newuser");
        findComponent(dialog, EmailField.class, field -> "Email".equals(field.getLabel())).setValue("new@example.com");
        findComponent(dialog, PasswordField.class, field -> "Password".equals(field.getLabel())).setValue("Secret123!");
        this.findRankComboBox(dialog).setValue(rank);

        findButton(dialog, "Save").click();

        final ArgumentCaptor<UserDto> userCaptor = ArgumentCaptor.forClass(UserDto.class);
        verify(this.userService).createUser(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("Secret123!");
        this.notificationMock.verify(() -> NotificationUtil.showSuccess("User created successfully"));
        verify(this.onSave).run();
    }

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    @Test
    void openPasswordDialog_shouldUseMaskedPasswordFields() throws Exception {
        final var dialog = new AdminUserDialog(this.userService, this.onSave, this.onAuthFailure);
        final var rank = this.createRank("1", "Administrator");
        final UserResponseDto existingUser =
                MockDataUtil.createUserResponseDto("1", "existinguser", "existing@example.com");

        dialog.open(existingUser, List.of(rank));
        findButton(dialog, "Change Password").click();

        final Field passwordDialogField = AdminUserDialog.class.getDeclaredField("passwordDialog");
        passwordDialogField.setAccessible(true);
        final FormDialog passwordDialog = (FormDialog) passwordDialogField.get(dialog);

        final List<PasswordField> passwordFields = findComponents(passwordDialog, PasswordField.class);
        assertThat(passwordFields).extracting(PasswordField::getLabel).containsExactlyInAnyOrder("New Password",
                "Confirm Password");
    }

    private UserRankResponseDto createRank(final String id, final String name) {
        final var rank = new UserRankResponseDto();
        rank.setPublicId(id);
        rank.setName(name);
        return rank;
    }

    @SuppressWarnings("unchecked")
    private ComboBox<UserRankResponseDto> findRankComboBox(final Component root) {
        return (ComboBox<UserRankResponseDto>) findComponent(root, ComboBox.class,
                field -> "Rank".equals(field.getLabel()));
    }

}
