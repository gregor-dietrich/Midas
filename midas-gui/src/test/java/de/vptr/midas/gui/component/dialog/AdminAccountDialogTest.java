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

import de.vptr.midas.gui.dto.AccountDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.service.AccountService;
import de.vptr.midas.gui.util.MockDataUtil;
import de.vptr.midas.gui.util.NotificationUtil;

@ExtendWith(MockitoExtension.class)
class AdminAccountDialogTest extends AbstractDialogTest {

    @Mock
    AccountService accountService;

    @Test
    void saveAccount_shouldCreateAccount_withValidFields() {
        final var dialog = new AdminAccountDialog(this.accountService, this.onSave, this.onAuthFailure);
        when(this.accountService.createAccount(any(AccountDto.class))).thenReturn(MockDataUtil.createAccountDto());

        dialog.open(null);

        findComponent(dialog, TextField.class, field -> "Account Name".equals(field.getLabel()))
                .setValue("New Savings Account");

        findButton(dialog, "Save").click();

        final ArgumentCaptor<AccountDto> captor = ArgumentCaptor.forClass(AccountDto.class);
        verify(this.accountService).createAccount(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("New Savings Account");
        this.notificationMock.verify(() -> NotificationUtil.showSuccess("Account created successfully"));
        verify(this.onSave).run();
    }

    @Test
    void saveAccount_shouldCallOnAuthFailure_onAuthException() {
        final var dialog = new AdminAccountDialog(this.accountService, this.onSave, this.onAuthFailure);
        when(this.accountService.createAccount(any(AccountDto.class)))
                .thenThrow(new AuthenticationException("Session expired"));

        dialog.open(null);

        findComponent(dialog, TextField.class, field -> "Account Name".equals(field.getLabel()))
                .setValue("Failing Account");

        findButton(dialog, "Save").click();

        this.notificationMock.verify(() -> NotificationUtil.showError("Session expired. Please log in again."));
        verify(this.onAuthFailure).run();
    }
}
