package de.vptr.midas.gui.component.dialog;

import static de.vptr.midas.gui.util.UiTestUtil.findButton;
import static de.vptr.midas.gui.util.UiTestUtil.findComponent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.textfield.TextField;

import de.vptr.midas.gui.dto.UserRankDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.service.UserRankService;
import de.vptr.midas.gui.util.MockDataUtil;
import de.vptr.midas.gui.util.NotificationUtil;

@ExtendWith(MockitoExtension.class)
class AdminUserRankDialogTest extends AbstractDialogTest {

    @Mock
    UserRankService userRankService;

    @Test
    void saveRank_shouldCreateNewRank_whenOpenedWithNull() {
        final var dialog = new AdminUserRankDialog(this.userRankService, this.onSave, this.onAuthFailure);
        when(this.userRankService.createRank(any(UserRankDto.class))).thenReturn(MockDataUtil.createUserRankDto());

        dialog.open(null);

        findComponent(dialog, TextField.class, field -> "Name".equals(field.getLabel())).setValue("Moderator");
        findComponent(dialog, Checkbox.class, cb -> "Can Add Users".equals(cb.getLabel())).setValue(true);
        findComponent(dialog, Checkbox.class, cb -> "Can Edit Posts".equals(cb.getLabel())).setValue(true);
        findComponent(dialog, Checkbox.class, cb -> "Admin View".equals(cb.getLabel())).setValue(true);

        findButton(dialog, "Save").click();

        final ArgumentCaptor<UserRankDto> captor = ArgumentCaptor.forClass(UserRankDto.class);
        verify(this.userRankService).createRank(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Moderator");
        assertThat(captor.getValue().isUserAdd()).isTrue();
        assertThat(captor.getValue().isPostEdit()).isTrue();
        assertThat(captor.getValue().isAdminView()).isTrue();
        this.notificationMock.verify(() -> NotificationUtil.showSuccess("Rank created successfully"));
        verify(this.onSave).run();
    }

    @Test
    void saveRank_shouldUpdateExistingRank_whenOpenedWithDto() {
        final var dialog = new AdminUserRankDialog(this.userRankService, this.onSave, this.onAuthFailure);
        final var existingRank = MockDataUtil.createUserRankResponseDto();
        when(this.userRankService.updateRank(any(UserRankDto.class))).thenReturn(MockDataUtil.createUserRankDto());

        dialog.open(existingRank);

        findComponent(dialog, TextField.class, field -> "Name".equals(field.getLabel())).setValue("Updated Rank");

        findButton(dialog, "Save").click();

        final ArgumentCaptor<UserRankDto> captor = ArgumentCaptor.forClass(UserRankDto.class);
        verify(this.userRankService).updateRank(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Updated Rank");
        assertThat(captor.getValue().getPublicId()).isEqualTo(existingRank.getPublicId());
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

        final var nameField = findComponent(dialog, TextField.class, field -> "Name".equals(field.getLabel()));
        assertThat(nameField.getValue()).isEqualTo(existingRank.getName());

        final var userAddCheckbox = findComponent(dialog, Checkbox.class, cb -> "Can Add Users".equals(cb.getLabel()));
        assertThat(userAddCheckbox.getValue()).isTrue();

        final var postEditCheckbox =
                findComponent(dialog, Checkbox.class, cb -> "Can Edit Posts".equals(cb.getLabel()));
        assertThat(postEditCheckbox.getValue()).isTrue();

        final var userDeleteCheckbox =
                findComponent(dialog, Checkbox.class, cb -> "Can Delete Users".equals(cb.getLabel()));
        assertThat(userDeleteCheckbox.getValue()).isFalse();
    }

    @Test
    void saveRank_shouldCallOnAuthFailure_whenAuthenticationExceptionThrown() {
        final var dialog = new AdminUserRankDialog(this.userRankService, this.onSave, this.onAuthFailure);
        when(this.userRankService.createRank(any(UserRankDto.class)))
                .thenThrow(new AuthenticationException("Session expired"));

        dialog.open(null);

        findComponent(dialog, TextField.class, field -> "Name".equals(field.getLabel())).setValue("New Rank");

        findButton(dialog, "Save").click();

        this.notificationMock.verify(() -> NotificationUtil.showError("Session expired. Please log in again."));
        verify(this.onAuthFailure).run();
    }
}
