package de.vptr.midas.gui.component.dialog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
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
class AdminUserDialogTest {

    @Mock
    UserService userService;

    private UI ui;
    private Runnable onSave;
    private Runnable onAuthFailure;
    private MockedStatic<NotificationUtil> notificationMock;

    @BeforeEach
    void setUp() {
        this.ui = new UI();
        UI.setCurrent(this.ui);
        this.onSave = mock(Runnable.class);
        this.onAuthFailure = mock(Runnable.class);
        this.notificationMock = mockStatic(NotificationUtil.class);
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
        this.notificationMock.close();
    }

    @Test
    void saveUser_shouldPreserveEnteredPassword_onCreate() {
        final var dialog = new AdminUserDialog(this.userService, this.onSave, this.onAuthFailure);
        final var rank = this.createRank(1L, "Administrator");
        when(this.userService.createUser(any(UserDto.class)))
                .thenReturn(MockDataUtil.createUserResponseDto(1L, "newuser", "new@example.com"));

        dialog.open(null, List.of(rank));

        this.findComponent(dialog, TextField.class, field -> "Username".equals(field.getLabel())).setValue("newuser");
        this.findComponent(dialog, EmailField.class, field -> "Email".equals(field.getLabel()))
                .setValue("new@example.com");
        this.findComponent(dialog, PasswordField.class, field -> "Password".equals(field.getLabel()))
                .setValue("secret123");
        this.findRankComboBox(dialog).setValue(rank);

        this.findButton(dialog, "Save").click();

        final ArgumentCaptor<UserDto> userCaptor = ArgumentCaptor.forClass(UserDto.class);
        verify(this.userService).createUser(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("secret123");
        this.notificationMock.verify(() -> NotificationUtil.showSuccess("User created successfully"));
        verify(this.onSave).run();
    }

    @Test
    void openPasswordDialog_shouldUseMaskedPasswordFields() throws Exception {
        final var dialog = new AdminUserDialog(this.userService, this.onSave, this.onAuthFailure);
        final var rank = this.createRank(1L, "Administrator");
        final UserResponseDto existingUser = MockDataUtil.createUserResponseDto(1L, "existinguser",
                "existing@example.com");

        dialog.open(existingUser, List.of(rank));
        this.findButton(dialog, "Change Password").click();

        final Field passwordDialogField = AdminUserDialog.class.getDeclaredField("passwordDialog");
        passwordDialogField.setAccessible(true);
        final FormDialog passwordDialog = (FormDialog) passwordDialogField.get(dialog);

        final List<PasswordField> passwordFields = this.findComponents(passwordDialog, PasswordField.class);
        assertThat(passwordFields)
                .extracting(PasswordField::getLabel)
                .containsExactlyInAnyOrder("New Password", "Confirm Password");
    }

    private UserRankResponseDto createRank(final Long id, final String name) {
        final var rank = new UserRankResponseDto();
        rank.setId(id);
        rank.setName(name);
        return rank;
    }

    private Button findButton(final Component root, final String text) {
        return this.findComponent(root, Button.class, button -> text.equals(button.getText()));
    }

    @SuppressWarnings("unchecked")
    private ComboBox<UserRankResponseDto> findRankComboBox(final Component root) {
        return (ComboBox<UserRankResponseDto>) this.findComponent(root, ComboBox.class,
                field -> "Rank".equals(field.getLabel()));
    }

    private <T extends Component> T findComponent(final Component root, final Class<T> type,
            final Predicate<T> predicate) {
        return this.findComponents(root, type).stream()
                .filter(predicate)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Component not found: " + type.getSimpleName()));
    }

    @SuppressWarnings("unchecked")
    private <T extends Component> List<T> findComponents(final Component root, final Class<T> type) {
        final List<T> directMatches = root.getChildren()
                .filter(type::isInstance)
                .map(component -> (T) component)
                .toList();
        final List<T> nestedMatches = root.getChildren()
                .flatMap(component -> this.findComponents(component, type).stream())
                .toList();
        return Stream.concat(directMatches.stream(), nestedMatches.stream()).toList();
    }
}