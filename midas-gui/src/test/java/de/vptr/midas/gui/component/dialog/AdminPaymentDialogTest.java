package de.vptr.midas.gui.component.dialog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;

import de.vptr.midas.gui.dto.AccountResponseDto;
import de.vptr.midas.gui.dto.PaymentDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.service.PaymentService;
import de.vptr.midas.gui.service.UserService;
import de.vptr.midas.gui.util.MockDataUtil;
import de.vptr.midas.gui.util.NotificationUtil;

@ExtendWith(MockitoExtension.class)
class AdminPaymentDialogTest {

        @Mock
        PaymentService paymentService;

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
        void savePayment_shouldCreatePayment_withValidFields() {
                final var dialog = new AdminPaymentDialog(this.paymentService, this.userService,
                                this.onSave, this.onAuthFailure);
                final var sourceAccount = MockDataUtil.createAccountResponseDto(1L, "Source Account");
                final var targetAccount = MockDataUtil.createAccountResponseDto(2L, "Target Account");
                final var accounts = List.of(sourceAccount, targetAccount);
                final var currentUser = MockDataUtil.createUserResponseDto();
                when(this.userService.getCurrentUser()).thenReturn(Optional.of(currentUser));
                when(this.paymentService.createPayment(any(PaymentDto.class)))
                                .thenReturn(new PaymentDto());

                dialog.open(null, accounts);

                this.findAccountComboBox(dialog, "Source Account").setValue(sourceAccount);
                this.findAccountComboBox(dialog, "Target Account").setValue(targetAccount);
                this.findComponent(dialog, BigDecimalField.class, field -> "Amount".equals(field.getLabel()))
                                .setValue(new BigDecimal("250.00"));
                this.findComponent(dialog, DatePicker.class, field -> "Payment Date".equals(field.getLabel()))
                                .setValue(LocalDate.now());
                this.findComponent(dialog, TextField.class, field -> "Comment".equals(field.getLabel()))
                                .setValue("Test payment");

                this.findButton(dialog, "Save").click();

                final ArgumentCaptor<PaymentDto> captor = ArgumentCaptor.forClass(PaymentDto.class);
                verify(this.paymentService).createPayment(captor.capture());
                assertThat(captor.getValue().getSourceAccountId()).isEqualTo(1L);
                assertThat(captor.getValue().getTargetAccountId()).isEqualTo(2L);
                assertThat(captor.getValue().getAmount()).isEqualByComparingTo(new BigDecimal("250.00"));
                assertThat(captor.getValue().getComment()).isEqualTo("Test payment");
                assertThat(captor.getValue().getUserId()).isEqualTo(currentUser.getId());
                this.notificationMock.verify(() -> NotificationUtil.showSuccess("Payment created successfully"));
                verify(this.onSave).run();
        }

        @Test
        void savePayment_shouldCallOnAuthFailure_onAuthException() {
                final var dialog = new AdminPaymentDialog(this.paymentService, this.userService,
                                this.onSave, this.onAuthFailure);
                final var sourceAccount = MockDataUtil.createAccountResponseDto(1L, "Source Account");
                final var targetAccount = MockDataUtil.createAccountResponseDto(2L, "Target Account");
                final var accounts = List.of(sourceAccount, targetAccount);
                final var currentUser = MockDataUtil.createUserResponseDto();
                when(this.userService.getCurrentUser()).thenReturn(Optional.of(currentUser));
                when(this.paymentService.createPayment(any(PaymentDto.class)))
                                .thenThrow(new AuthenticationException("Session expired"));

                dialog.open(null, accounts);

                this.findAccountComboBox(dialog, "Source Account").setValue(sourceAccount);
                this.findAccountComboBox(dialog, "Target Account").setValue(targetAccount);
                this.findComponent(dialog, BigDecimalField.class, field -> "Amount".equals(field.getLabel()))
                                .setValue(new BigDecimal("100.00"));
                this.findComponent(dialog, DatePicker.class, field -> "Payment Date".equals(field.getLabel()))
                                .setValue(LocalDate.now());
                this.findComponent(dialog, TextField.class, field -> "Comment".equals(field.getLabel()))
                                .setValue("Auth test");

                this.findButton(dialog, "Save").click();

                this.notificationMock
                                .verify(() -> NotificationUtil.showError("Session expired. Please log in again."));
                verify(this.onAuthFailure).run();
        }

        @Test
        void open_shouldPopulateFieldsForEdit() {
                final var dialog = new AdminPaymentDialog(this.paymentService, this.userService,
                                this.onSave, this.onAuthFailure);
                final var sourceAccount = MockDataUtil.createAccountResponseDto(1L, "Source Account");
                final var targetAccount = MockDataUtil.createAccountResponseDto(2L, "Target Account");
                final var accounts = List.of(sourceAccount, targetAccount);

                final var existingPayment = new PaymentDto();
                existingPayment.setId(5L);
                existingPayment.setUserId(1L);
                existingPayment.setSourceAccountId(1L);
                existingPayment.setTargetAccountId(2L);
                existingPayment.setAmount(new BigDecimal("500.00"));
                existingPayment.setDate(LocalDate.of(2024, 6, 15));
                existingPayment.setComment("Existing payment");

                dialog.open(existingPayment, accounts);

                final var amountField = this.findComponent(dialog, BigDecimalField.class,
                                field -> "Amount".equals(field.getLabel()));
                assertThat(amountField.getValue()).isEqualByComparingTo(new BigDecimal("500.00"));

                final var dateField = this.findComponent(dialog, DatePicker.class,
                                field -> "Payment Date".equals(field.getLabel()));
                assertThat(dateField.getValue()).isEqualTo(LocalDate.of(2024, 6, 15));

                final var commentField = this.findComponent(dialog, TextField.class,
                                field -> "Comment".equals(field.getLabel()));
                assertThat(commentField.getValue()).isEqualTo("Existing payment");

                final var sourceCombo = this.findAccountComboBox(dialog, "Source Account");
                assertThat(sourceCombo.getValue()).isNotNull();
                assertThat(sourceCombo.getValue().getId()).isEqualTo(1L);

                final var targetCombo = this.findAccountComboBox(dialog, "Target Account");
                assertThat(targetCombo.getValue()).isNotNull();
                assertThat(targetCombo.getValue().getId()).isEqualTo(2L);
        }

        private Button findButton(final Component root, final String text) {
                return this.findComponent(root, Button.class, button -> text.equals(button.getText()));
        }

        @SuppressWarnings("unchecked")
        private ComboBox<AccountResponseDto> findAccountComboBox(final Component root, final String label) {
                return (ComboBox<AccountResponseDto>) this.findComponent(root, ComboBox.class,
                                field -> label.equals(field.getLabel()));
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
