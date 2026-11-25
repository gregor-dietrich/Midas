package de.vptr.midas.gui.component.dialog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import jakarta.annotation.Nullable;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;

import de.vptr.midas.gui.dto.AccountResponseDto;
import de.vptr.midas.gui.dto.PaymentDto;
import de.vptr.midas.gui.service.PaymentService;
import de.vptr.midas.gui.util.NotificationUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Dialog for creating and editing payments in the admin view. Encapsulates form fields, binder, and save/cancel logic.
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Service reference intentionally stored for runtime behavior")
public class AdminPaymentDialog extends AbstractAdminEntityDialogBase<PaymentDto> {

    private final transient PaymentService paymentService;

    /** Initializes AdminPaymentDialog. */
    public AdminPaymentDialog(final PaymentService paymentService, final Runnable onSave,
            final Runnable onAuthFailure) {
        super(onSave, onAuthFailure);
        this.paymentService = paymentService;
    }

    /** Opens the dialog for creating or editing a payment. */
    public void open(@Nullable final PaymentDto payment, @Nullable final List<AccountResponseDto> availableAccounts) {
        this.removeAll();
        final var bean = payment != null ? payment : new PaymentDto();
        this.currentBean = bean;
        final var b = new Binder<>(PaymentDto.class);
        this.binder = b;

        final var form = this.createResponsiveFormLayout();
        final var accounts = availableAccounts != null ? availableAccounts : List.<AccountResponseDto>of();

        final var sourceAccountField = createAccountComboBox("Source Account", "Select source account...", accounts);
        final var targetAccountField = createAccountComboBox("Target Account", "Select target account...", accounts);
        final var amountField = new BigDecimalField("Amount");
        final var dateField = new DatePicker("Payment Date");
        final var commentField = new TextField("Comment");
        commentField.setMaxLength(255);

        amountField.setInvalid(false);
        dateField.setInvalid(false);
        commentField.setInvalid(false);

        preventSameAccountSelection(sourceAccountField, targetAccountField,
                "Source and target accounts cannot be the same");
        bindPaymentFields(b, sourceAccountField, targetAccountField, amountField, dateField, commentField, accounts);

        form.add(sourceAccountField, targetAccountField, amountField, dateField, commentField);
        applyInitialAccounts(bean, sourceAccountField, targetAccountField, accounts);

        this.initDialogLayout(payment != null ? "Edit Payment" : "Create Payment", form);
    }

    private static ComboBox<AccountResponseDto> createAccountComboBox(final String label, final String placeholder,
            final List<AccountResponseDto> accounts) {
        final var field = new ComboBox<AccountResponseDto>(label);
        field.setItems(accounts);
        field.setItemLabelGenerator(account -> account.getPublicId() + " - " + account.getName());
        field.setPlaceholder(placeholder);
        field.setInvalid(false);
        return field;
    }

    /** Prevents selecting the same account in both combo boxes, clearing the changed field with a warning. */
    public static void preventSameAccountSelection(final ComboBox<AccountResponseDto> sourceAccountField,
            final ComboBox<AccountResponseDto> targetAccountField, final String warningMessage) {
        sourceAccountField.addValueChangeListener(
                e -> clearIfSameAccount(e.getValue(), targetAccountField, sourceAccountField, warningMessage));
        targetAccountField.addValueChangeListener(
                e -> clearIfSameAccount(e.getValue(), sourceAccountField, targetAccountField, warningMessage));
    }

    private static void clearIfSameAccount(@Nullable final AccountResponseDto selected,
            final ComboBox<AccountResponseDto> otherField, final ComboBox<AccountResponseDto> currentField,
            final String warningMessage) {
        if (selected != null && selected.equals(otherField.getValue())) {
            NotificationUtil.showWarning(warningMessage);
            currentField.setValue(null);
        }
    }

    private static void bindPaymentFields(final Binder<PaymentDto> b,
            final ComboBox<AccountResponseDto> sourceAccountField,
            final ComboBox<AccountResponseDto> targetAccountField, final BigDecimalField amountField,
            final DatePicker dateField, final TextField commentField, final List<AccountResponseDto> accounts) {
        b.forField(sourceAccountField).bind(p -> findAccountByPublicId(accounts, p.getSourceAccountPublicId()),
                (p, value) -> p.setSourceAccountPublicId(value != null ? value.getPublicId() : null));

        b.forField(targetAccountField).bind(p -> findAccountByPublicId(accounts, p.getTargetAccountPublicId()),
                (p, value) -> p.setTargetAccountPublicId(value != null ? value.getPublicId() : null));

        b.forField(amountField).withValidator(value -> value != null && value.compareTo(BigDecimal.ZERO) != 0,
                "Amount must not equal 0").bind(PaymentDto::getAmount, PaymentDto::setAmount);

        b.forField(dateField).withValidator(value -> value != null, "Payment date is required")
                .bind(PaymentDto::getDate, PaymentDto::setDate);

        b.forField(commentField)
                .withValidator(value -> value != null && !value.trim().isEmpty(), "Payment comment is required")
                .bind(PaymentDto::getComment, PaymentDto::setComment);
    }

    private static void applyInitialAccounts(final PaymentDto bean,
            final ComboBox<AccountResponseDto> sourceAccountField,
            final ComboBox<AccountResponseDto> targetAccountField, final List<AccountResponseDto> accounts) {
        if (bean.getSourceAccountPublicId() != null) {
            sourceAccountField.setValue(findAccountByPublicId(accounts, bean.getSourceAccountPublicId()));
        }
        if (bean.getTargetAccountPublicId() != null) {
            targetAccountField.setValue(findAccountByPublicId(accounts, bean.getTargetAccountPublicId()));
        }
    }

    @Nullable
    private static AccountResponseDto findAccountByPublicId(final List<AccountResponseDto> accounts,
            @Nullable final String accountPublicId) {
        if (accountPublicId == null) {
            return null;
        }
        return accounts.stream().filter(account -> account.getPublicId().equals(accountPublicId)).findFirst()
                .orElse(null);
    }

    @Override
    protected void handleSave() {
        final boolean isNew = Objects.requireNonNull(this.currentBean).getPublicId() == null;
        this.executeSaveWorkflow(isNew, "Payment created successfully", "Payment updated successfully",
                this.paymentService::createPayment, this.paymentService::updatePayment, "Payment");
    }

    @Override
    protected boolean preSave(final PaymentDto bean) {
        if (bean.getSourceAccountPublicId() == null && bean.getTargetAccountPublicId() == null) {
            NotificationUtil.showError("At least one account (source or target) must be selected");
            return false;
        }

        return true;
    }
}
