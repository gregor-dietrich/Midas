package de.vptr.midas.gui.component.dialog;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;

import de.vptr.midas.gui.dto.AccountDto;
import de.vptr.midas.gui.dto.AccountResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.AccountService;
import de.vptr.midas.gui.util.NotificationUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;

/**
 * Dialog for creating and editing accounts in the admin view. Encapsulates form fields, binder, and save/cancel logic.
 */
@SuppressWarnings("NullAway")
@SuppressFBWarnings(value = { "EI_EXPOSE_REP2", "UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR" },
        justification = "Fields initialized lazily in open(), not constructor")
public class AdminAccountDialog extends FormDialog {

    private static final Logger LOG = Logger.getLogger(AdminAccountDialog.class);

    private final transient AccountService accountService;
    private final Runnable onSave;
    private final Runnable onAuthFailure;

    private Binder<AccountDto> binder;
    private transient AccountDto currentAccount;

    /** Initializes AdminAccountDialog. */
    public AdminAccountDialog(final AccountService accountService, final Runnable onSave,
            final Runnable onAuthFailure) {
        super();
        this.accountService = accountService;
        this.onSave = onSave;
        this.onAuthFailure = onAuthFailure;
    }

    /** Opens the dialog for creating or editing an account. */
    public void open(@Nullable final AccountResponseDto account) {
        this.removeAll();
        this.currentAccount = account != null ? account.toAccountDto() : new AccountDto();
        this.binder = new Binder<>(AccountDto.class);

        final var title = new H3(account != null ? "Edit Account" : "Create Account");

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        final var nameField = new TextField("Account Name");
        nameField.setRequired(true);
        nameField.setMaxLength(255);
        nameField.setInvalid(false);

        this.binder.forField(nameField)
                .withValidator(value -> value != null && !value.trim().isEmpty(), "Name is required")
                .bind(AccountDto::getName, AccountDto::setName);

        form.add(nameField);

        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var saveButton = new Button("Save", _ -> this.saveAccount());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var cancelButton = new Button("Cancel", _ -> this.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttonLayout.add(saveButton, cancelButton);

        final var dialogLayout = new VerticalLayout(title, form, buttonLayout);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);

        this.add(dialogLayout);
        this.binder.readBean(this.currentAccount);
        super.open();
    }

    private void saveAccount() {
        try {
            if (!this.binder.validate().isOk()) {
                NotificationUtil.showError("Please check the form for errors");
                return;
            }

            this.binder.writeBean(this.currentAccount);

            if (this.currentAccount.getId() == null) {
                this.accountService.createAccount(this.currentAccount);
                NotificationUtil.showSuccess("Account created successfully");
            } else {
                this.accountService.updateAccount(this.currentAccount);
                NotificationUtil.showSuccess("Account updated successfully");
            }

            this.close();
            this.onSave.run();

        } catch (final ValidationException e) {
            NotificationUtil.showError("Please check the form for errors");
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.onAuthFailure.run();
        } catch (final ServiceException e) {
            NotificationUtil.showError(e, "Saving account failed. Please try again.");
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error saving account", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }
}
