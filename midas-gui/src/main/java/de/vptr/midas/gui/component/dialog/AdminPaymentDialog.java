package de.vptr.midas.gui.component.dialog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;

import de.vptr.midas.gui.dto.AccountResponseDto;
import de.vptr.midas.gui.dto.PaymentDto;
import de.vptr.midas.gui.service.PaymentService;
import de.vptr.midas.gui.util.NotificationUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;

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
        this.currentBean = payment != null ? payment : new PaymentDto();
        this.binder = new Binder<>(PaymentDto.class);

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        final var sourceAccountField = new ComboBox<AccountResponseDto>("Source Account");
        final var targetAccountField = new ComboBox<AccountResponseDto>("Target Account");
        final var amountField = new BigDecimalField("Amount");
        final var dateField = new DatePicker("Payment Date");
        final var commentField = new TextField("Comment");
        commentField.setMaxLength(255);

        final var accounts = availableAccounts != null ? availableAccounts : List.<AccountResponseDto>of();
        sourceAccountField.setItems(accounts);
        sourceAccountField.setItemLabelGenerator(account -> account.getPublicId() + " - " + account.getName());
        sourceAccountField.setPlaceholder("Select source account...");
        sourceAccountField.setInvalid(false);

        targetAccountField.setItems(accounts);
        targetAccountField.setItemLabelGenerator(account -> account.getPublicId() + " - " + account.getName());
        targetAccountField.setPlaceholder("Select target account...");
        targetAccountField.setInvalid(false);

        amountField.setInvalid(false);
        dateField.setInvalid(false);
        commentField.setInvalid(false);

        sourceAccountField.addValueChangeListener(e -> {
            if (e.getValue() != null && e.getValue().equals(targetAccountField.getValue())) {
                NotificationUtil.showWarning("Source and target accounts cannot be the same");
                sourceAccountField.setValue(null);
            }
        });

        targetAccountField.addValueChangeListener(e -> {
            if (e.getValue() != null && e.getValue().equals(sourceAccountField.getValue())) {
                NotificationUtil.showWarning("Source and target accounts cannot be the same");
                targetAccountField.setValue(null);
            }
        });

        this.binder.forField(sourceAccountField).bind(
                p -> findAccountByPublicId(accounts, p.getSourceAccountPublicId()),
                (p, value) -> p.setSourceAccountPublicId(value != null ? value.getPublicId() : null));

        this.binder.forField(targetAccountField).bind(
                p -> findAccountByPublicId(accounts, p.getTargetAccountPublicId()),
                (p, value) -> p.setTargetAccountPublicId(value != null ? value.getPublicId() : null));

        this.binder.forField(amountField).withValidator(value -> value != null && value.compareTo(BigDecimal.ZERO) != 0,
                "Amount must not equal 0").bind(PaymentDto::getAmount, PaymentDto::setAmount);

        this.binder.forField(dateField).withValidator(value -> value != null, "Payment date is required")
                .bind(PaymentDto::getDate, PaymentDto::setDate);

        this.binder.forField(commentField)
                .withValidator(value -> value != null && !value.trim().isEmpty(), "Payment comment is required")
                .bind(PaymentDto::getComment, PaymentDto::setComment);

        form.add(sourceAccountField, targetAccountField, amountField, dateField, commentField);

        final var bean = Objects.requireNonNull(this.currentBean);
        if (bean.getSourceAccountPublicId() != null) {
            sourceAccountField.setValue(findAccountByPublicId(accounts, bean.getSourceAccountPublicId()));
        }
        if (bean.getTargetAccountPublicId() != null) {
            targetAccountField.setValue(findAccountByPublicId(accounts, bean.getTargetAccountPublicId()));
        }

        this.initDialogLayout(payment != null ? "Edit Payment" : "Create Payment", form);
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
