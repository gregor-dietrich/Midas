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

import com.vaadin.flow.component.textfield.TextField;

import de.vptr.midas.gui.dto.UserGroupDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.service.UserGroupService;
import de.vptr.midas.gui.util.MockDataUtil;
import de.vptr.midas.gui.util.NotificationUtil;

@ExtendWith(MockitoExtension.class)
class AdminUserGroupDialogTest extends AbstractDialogTest {

    @Mock
    UserGroupService groupService;

    @Test
    void saveGroup_shouldCreateGroup_withValidFields() {
        final var dialog = new AdminUserGroupDialog(this.groupService, this.onSave, this.onAuthFailure);
        when(this.groupService.createGroup(any(UserGroupDto.class))).thenReturn(MockDataUtil.createUserGroupDto());

        dialog.open(null);

        findComponent(dialog, TextField.class, field -> "Name".equals(field.getLabel())).setValue("Administrators");

        findButton(dialog, "Save").click();

        final ArgumentCaptor<UserGroupDto> captor = ArgumentCaptor.forClass(UserGroupDto.class);
        verify(this.groupService).createGroup(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Administrators");
        this.notificationMock.verify(() -> NotificationUtil.showSuccess("Group created successfully"));
        verify(this.onSave).run();
    }

    @Test
    void saveGroup_shouldCallOnAuthFailure_onAuthException() {
        final var dialog = new AdminUserGroupDialog(this.groupService, this.onSave, this.onAuthFailure);
        when(this.groupService.createGroup(any(UserGroupDto.class)))
                .thenThrow(new AuthenticationException("Session expired"));

        dialog.open(null);

        findComponent(dialog, TextField.class, field -> "Name".equals(field.getLabel())).setValue("Failing Group");

        findButton(dialog, "Save").click();

        this.notificationMock.verify(() -> NotificationUtil.showError("Session expired. Please log in again."));
        verify(this.onAuthFailure).run();
    }
}
