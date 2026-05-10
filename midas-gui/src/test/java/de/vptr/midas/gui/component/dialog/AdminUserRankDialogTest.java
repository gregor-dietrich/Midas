package de.vptr.midas.gui.component.dialog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.textfield.TextField;

import de.vptr.midas.gui.dto.UserRankDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.service.UserRankService;
import de.vptr.midas.gui.util.MockDataUtil;
import de.vptr.midas.gui.util.NotificationUtil;

@ExtendWith(MockitoExtension.class)
class AdminUserRankDialogTest {

    @Mock
    UserRankService userRankService;

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
    void saveRank_shouldCreateNewRank_whenOpenedWithNull() {
        final var dialog = new AdminUserRankDialog(this.userRankService, this.onSave, this.onAuthFailure);
        when(this.userRankService.createRank(any(UserRankDto.class))).thenReturn(MockDataUtil.createUserRankDto());

        dialog.open(null);

        this.findComponent(dialog, TextField.class, field -> "Name".equals(field.getLabel())).setValue("Moderator");
        this.findComponent(dialog, Checkbox.class, cb -> "Can Add Users".equals(cb.getLabel())).setValue(true);
        this.findComponent(dialog, Checkbox.class, cb -> "Can Edit Posts".equals(cb.getLabel())).setValue(true);
        this.findComponent(dialog, Checkbox.class, cb -> "Admin View".equals(cb.getLabel())).setValue(true);

        this.findButton(dialog, "Save").click();

        final ArgumentCaptor<UserRankDto> captor = ArgumentCaptor.forClass(UserRankDto.class);
        verify(this.userRankService).createRank(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Moderator");
        assertThat(captor.getValue().getUserAdd()).isTrue();
        assertThat(captor.getValue().getPostEdit()).isTrue();
        assertThat(captor.getValue().getAdminView()).isTrue();
        this.notificationMock.verify(() -> NotificationUtil.showSuccess("Rank created successfully"));
        verify(this.onSave).run();
    }

    @Test
    void saveRank_shouldUpdateExistingRank_whenOpenedWithDto() {
        final var dialog = new AdminUserRankDialog(this.userRankService, this.onSave, this.onAuthFailure);
        final var existingRank = MockDataUtil.createUserRankResponseDto();
        when(this.userRankService.updateRank(any(UserRankDto.class))).thenReturn(MockDataUtil.createUserRankDto());

        dialog.open(existingRank);

        this.findComponent(dialog, TextField.class, field -> "Name".equals(field.getLabel())).setValue("Updated Rank");

        this.findButton(dialog, "Save").click();

        final ArgumentCaptor<UserRankDto> captor = ArgumentCaptor.forClass(UserRankDto.class);
        verify(this.userRankService).updateRank(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Updated Rank");
        assertThat(captor.getValue().getId()).isEqualTo(existingRank.getId());
        this.notificationMock.verify(() -> NotificationUtil.showSuccess("Rank updated successfully"));
        verify(this.onSave).run();
    }

    @Test
    void open_shouldPopulateFieldsFromExistingRank() {
        final var dialog = new AdminUserRankDialog(this.userRankService, this.onSave, this.onAuthFailure);
        final var existingRank = MockDataUtil.createUserRankResponseDto();
        existingRank.setUserAdd(true);
        existingRank.setPostEdit(true);
        existingRank.setUserDelete(false);

        dialog.open(existingRank);

        final var nameField = this.findComponent(dialog, TextField.class, field -> "Name".equals(field.getLabel()));
        assertThat(nameField.getValue()).isEqualTo(existingRank.getName());

        final var userAddCheckbox =
                this.findComponent(dialog, Checkbox.class, cb -> "Can Add Users".equals(cb.getLabel()));
        assertThat(userAddCheckbox.getValue()).isTrue();

        final var postEditCheckbox =
                this.findComponent(dialog, Checkbox.class, cb -> "Can Edit Posts".equals(cb.getLabel()));
        assertThat(postEditCheckbox.getValue()).isTrue();

        final var userDeleteCheckbox =
                this.findComponent(dialog, Checkbox.class, cb -> "Can Delete Users".equals(cb.getLabel()));
        assertThat(userDeleteCheckbox.getValue()).isFalse();
    }

    @Test
    void saveRank_shouldCallOnAuthFailure_whenAuthenticationExceptionThrown() {
        final var dialog = new AdminUserRankDialog(this.userRankService, this.onSave, this.onAuthFailure);
        when(this.userRankService.createRank(any(UserRankDto.class)))
                .thenThrow(new AuthenticationException("Session expired"));

        dialog.open(null);

        this.findComponent(dialog, TextField.class, field -> "Name".equals(field.getLabel())).setValue("New Rank");

        this.findButton(dialog, "Save").click();

        this.notificationMock.verify(() -> NotificationUtil.showError("Session expired. Please log in again."));
        verify(this.onAuthFailure).run();
    }

    private Button findButton(final Component root, final String text) {
        return this.findComponent(root, Button.class, button -> text.equals(button.getText()));
    }

    private <T extends Component> T findComponent(final Component root, final Class<T> type,
            final Predicate<T> predicate) {
        return this.findComponents(root, type).stream().filter(predicate).findFirst()
                .orElseThrow(() -> new AssertionError("Component not found: " + type.getSimpleName()));
    }

    @SuppressWarnings("unchecked")
    private <T extends Component> List<T> findComponents(final Component root, final Class<T> type) {
        final List<T> directMatches =
                root.getChildren().filter(type::isInstance).map(component -> (T) component).toList();
        final List<T> nestedMatches =
                root.getChildren().flatMap(component -> this.findComponents(component, type).stream()).toList();
        return Stream.concat(directMatches.stream(), nestedMatches.stream()).toList();
    }
}
