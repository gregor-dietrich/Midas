package de.vptr.midas.gui.component.dialog;

import static de.vptr.midas.gui.util.UiTestUtil.findButton;
import static de.vptr.midas.gui.util.UiTestUtil.findComponent;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;

import de.vptr.midas.gui.dto.AccountResponseDto;
import de.vptr.midas.gui.dto.PaymentDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.service.PaymentService;
import de.vptr.midas.gui.util.MockDataUtil;
import de.vptr.midas.gui.util.NotificationUtil;

@ExtendWith(MockitoExtension.class)
class AdminPaymentDialogTest extends AbstractDialogTest {

    @Mock
    PaymentService paymentService;

    AdminPaymentDialog dialog;
    AccountResponseDto sourceAccount;
    AccountResponseDto targetAccount;

    @BeforeEach
    void setUpDialog() {
        this.dialog = new AdminPaymentDialog(this.paymentService, this.onSave, this.onAuthFailure);
        this.sourceAccount = MockDataUtil.createAccountResponseDto(1L, "Source Account");
        this.targetAccount = MockDataUtil.createAccountResponseDto(2L, "Target Account");
    }

    @Test
    void savePayment_shouldCreatePayment_withValidFields() {
        when(this.paymentService.createPayment(any(PaymentDto.class))).thenReturn(new PaymentDto());

        this.setupAndSubmitPayment(this.dialog, this.sourceAccount, this.targetAccount, "250.00", "Test payment");

        final ArgumentCaptor<PaymentDto> captor = ArgumentCaptor.forClass(PaymentDto.class);
        verify(this.paymentService).createPayment(captor.capture());
        assertThat(captor.getValue().getSourceAccountPublicId()).isEqualTo("1");
        assertThat(captor.getValue().getTargetAccountPublicId()).isEqualTo("2");
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
        assertThat(captor.getValue().getComment()).isEqualTo("Test payment");
        this.notificationMock.verify(() -> NotificationUtil.showSuccess("Payment created successfully"));
        verify(this.onSave).run();
    }

    @Test
    void savePayment_shouldCallOnAuthFailure_onAuthException() {
        when(this.paymentService.createPayment(any(PaymentDto.class)))
                .thenThrow(new AuthenticationException("Session expired"));

        this.setupAndSubmitPayment(this.dialog, this.sourceAccount, this.targetAccount, "100.00", "Auth test");

        this.notificationMock.verify(() -> NotificationUtil.showError("Session expired. Please log in again."));
        verify(this.onAuthFailure).run();
    }

    private void setupAndSubmitPayment(final AdminPaymentDialog dialog, final AccountResponseDto sourceAccount,
            final AccountResponseDto targetAccount, final String amount, final String comment) {
        final var accounts = List.of(sourceAccount, targetAccount);

        dialog.open(null, accounts);

        this.findAccountComboBox(dialog, "Source Account").setValue(sourceAccount);
        this.findAccountComboBox(dialog, "Target Account").setValue(targetAccount);
        findComponent(dialog, BigDecimalField.class, field -> "Amount".equals(field.getLabel()))
                .setValue(new BigDecimal(amount));
        findComponent(dialog, DatePicker.class, field -> "Payment Date".equals(field.getLabel()))
                .setValue(LocalDate.now(ZoneId.systemDefault()));
        findComponent(dialog, TextField.class, field -> "Comment".equals(field.getLabel())).setValue(comment);

        findButton(dialog, "Save").click();
    }

    @Test
    void open_shouldPopulateFieldsForEdit() {
        final var dialog = new AdminPaymentDialog(this.paymentService, this.onSave, this.onAuthFailure);
        final var sourceAccount = MockDataUtil.createAccountResponseDto(1L, "Source Account");
        final var targetAccount = MockDataUtil.createAccountResponseDto(2L, "Target Account");
        final var accounts = List.of(sourceAccount, targetAccount);

        final var existingPayment = new PaymentDto();
        existingPayment.setPublicId("5");
        existingPayment.setSourceAccountPublicId("1");
        existingPayment.setTargetAccountPublicId("2");
        existingPayment.setAmount(new BigDecimal("500.00"));
        existingPayment.setDate(LocalDate.of(2024, 6, 15));
        existingPayment.setComment("Existing payment");

        dialog.open(existingPayment, accounts);

        final var amountField =
                findComponent(dialog, BigDecimalField.class, field -> "Amount".equals(field.getLabel()));
        assertThat(amountField.getValue()).isEqualByComparingTo(new BigDecimal("500.00"));

        final var dateField = findComponent(dialog, DatePicker.class, field -> "Payment Date".equals(field.getLabel()));
        assertThat(dateField.getValue()).isEqualTo(LocalDate.of(2024, 6, 15));

        final var commentField = findComponent(dialog, TextField.class, field -> "Comment".equals(field.getLabel()));
        assertThat(commentField.getValue()).isEqualTo("Existing payment");

        final var sourceCombo = this.findAccountComboBox(dialog, "Source Account");
        assertThat(sourceCombo.getValue()).isNotNull();
        assertThat(sourceCombo.getValue().getPublicId()).isEqualTo("1");

        final var targetCombo = this.findAccountComboBox(dialog, "Target Account");
        assertThat(targetCombo.getValue()).isNotNull();
        assertThat(targetCombo.getValue().getPublicId()).isEqualTo("2");
    }

    @SuppressWarnings("unchecked")
    private ComboBox<AccountResponseDto> findAccountComboBox(final Component root, final String label) {
        return (ComboBox<AccountResponseDto>) findComponent(root, ComboBox.class,
                field -> label.equals(field.getLabel()));
    }
}
