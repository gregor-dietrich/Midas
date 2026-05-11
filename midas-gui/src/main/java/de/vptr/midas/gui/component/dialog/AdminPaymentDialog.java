package de.vptr.midas.gui.component.dialog;

import java.math.BigDecimal;
import java.util.List;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;

import de.vptr.midas.gui.dto.AccountResponseDto;
import de.vptr.midas.gui.dto.PaymentDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.PaymentService;
import de.vptr.midas.gui.service.UserService;
import de.vptr.midas.gui.util.NotificationUtil;
import jakarta.annotation.Nullable;

/**
 * Dialog for creating and editing payments in the admin view. Encapsulates form fields, binder, and save/cancel logic.
 */
public class AdminPaymentDialog extends FormDialog {

    private static final Logger LOG = Logger.getLogger(AdminPaymentDialog.class);

    private final PaymentService paymentService;
    private final UserService userService;
    private final Runnable onSave;
    private final Runnable onAuthFailure;

    private Binder<PaymentDto> binder;
    private PaymentDto currentPayment;

    /** Initializes AdminPaymentDialog. */
    public AdminPaymentDialog(final PaymentService paymentService, final UserService userService, final Runnable onSave,
            final Runnable onAuthFailure) {
        super();
        this.paymentService = paymentService;
        this.userService = userService;
        this.onSave = onSave;
        this.onAuthFailure = onAuthFailure;
    }

    /** Opens the dialog for creating or editing a payment. */
    public void open(final @Nullable PaymentDto payment, final @Nullable List<AccountResponseDto> availableAccounts) {
        this.removeAll();
        this.currentPayment = payment != null ? payment : new PaymentDto();
        this.binder = new Binder<>(PaymentDto.class);

        final var title = new H3(payment != null ? "Edit Payment" : "Create Payment");

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        final var sourceAccountField = new ComboBox<AccountResponseDto>("Source Account");
        final var targetAccountField = new ComboBox<AccountResponseDto>("Target Account");
        final var amountField = new BigDecimalField("Amount");
        final var dateField = new DatePicker("Payment Date");
        final var commentField = new TextField("Comment");
        commentField.setMaxLength(255);

        // Configure account dropdowns
        final var accounts = availableAccounts != null ? availableAccounts : List.<AccountResponseDto>of();
        sourceAccountField.setItems(accounts);
        sourceAccountField.setItemLabelGenerator(account -> account.getId() + " - " + account.getName());
        sourceAccountField.setPlaceholder("Select source account...");
        sourceAccountField.setInvalid(false);

        targetAccountField.setItems(accounts);
        targetAccountField.setItemLabelGenerator(account -> account.getId() + " - " + account.getName());
        targetAccountField.setPlaceholder("Select target account...");
        targetAccountField.setInvalid(false);

        amountField.setInvalid(false);
        dateField.setInvalid(false);
        commentField.setInvalid(false);

        // Prevent same account selection
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

        // Bind fields
        this.binder.forField(sourceAccountField).bind(_ -> null,
                (p, value) -> p.setSourceAccountId(value != null ? value.getId() : null));

        this.binder.forField(targetAccountField).bind(_ -> null,
                (p, value) -> p.setTargetAccountId(value != null ? value.getId() : null));

        this.binder.forField(amountField).withValidator(value -> value != null && value.compareTo(BigDecimal.ZERO) != 0,
                "Amount must not equal 0").bind(PaymentDto::getAmount, PaymentDto::setAmount);

        this.binder.forField(dateField).withValidator(value -> value != null, "Payment date is required")
                .bind(PaymentDto::getDate, PaymentDto::setDate);

        this.binder.forField(commentField)
                .withValidator(value -> value != null && !value.trim().isEmpty(), "Payment comment is required")
                .bind(PaymentDto::getComment, PaymentDto::setComment);

        form.add(sourceAccountField, targetAccountField, amountField, dateField, commentField);

        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var saveButton = new Button("Save", _ -> this.savePayment());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var cancelButton = new Button("Cancel", _ -> this.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttonLayout.add(saveButton, cancelButton);

        final var dialogLayout = new VerticalLayout(title, form, buttonLayout);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);

        this.add(dialogLayout);
        this.binder.readBean(this.currentPayment);

        // Set account fields for editing
        if (this.currentPayment.getSourceAccountId() != null) {
            sourceAccountField.setValue(findAccountById(accounts, this.currentPayment.getSourceAccountId()));
        }
        if (this.currentPayment.getTargetAccountId() != null) {
            targetAccountField.setValue(findAccountById(accounts, this.currentPayment.getTargetAccountId()));
        }

        super.open();
    }

    private void savePayment() {
        try {
            if (!this.binder.validate().isOk()) {
                NotificationUtil.showError("Please check the form for errors");
                return;
            }

            this.binder.writeBean(this.currentPayment);

            if (this.currentPayment.getSourceAccountId() == null && this.currentPayment.getTargetAccountId() == null) {
                NotificationUtil.showError("At least one account (source or target) must be selected");
                return;
            }

            // Ensure userId is set for API validation
            if (this.currentPayment.getUserId() == null) {
                final var currentUser = this.userService.getCurrentUser();
                if (currentUser.isPresent()) {
                    this.currentPayment.setUserId(currentUser.get().getId());
                } else {
                    NotificationUtil.showError("Unable to determine current user");
                    return;
                }
            }

            if (this.currentPayment.getId() == null) {
                this.paymentService.createPayment(this.currentPayment);
                NotificationUtil.showSuccess("Payment created successfully");
            } else {
                this.paymentService.updatePayment(this.currentPayment);
                NotificationUtil.showSuccess("Payment updated successfully");
            }

            this.close();
            this.onSave.run();

        } catch (final ValidationException e) {
            NotificationUtil.showError("Please check the form for errors");
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.onAuthFailure.run();
        } catch (final ServiceException e) {
            NotificationUtil.showError(e, "Saving payment failed. Please try again.");
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error saving payment", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private static AccountResponseDto findAccountById(final List<AccountResponseDto> accounts, final Long accountId) {
        if (accountId == null) {
            return null;
        }
        return accounts.stream().filter(account -> account.getId().equals(accountId)).findFirst().orElse(null);
    }
}
