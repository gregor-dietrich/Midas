package de.vptr.midas.gui.component.dialog;

import com.vaadin.flow.data.binder.Binder;

import de.vptr.midas.gui.dto.AccountDto;
import de.vptr.midas.gui.dto.AccountResponseDto;
import de.vptr.midas.gui.service.AccountService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;

/**
 * Dialog for creating and editing accounts in the admin view. Encapsulates form fields, binder, and save/cancel logic.
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "Service reference intentionally stored for runtime behavior")
public class AdminAccountDialog extends AbstractAdminEntityDialogBase<AccountDto> {

    private final transient AccountService accountService;
    private boolean isNewAccount;

    /** Initializes AdminAccountDialog. */
    public AdminAccountDialog(final AccountService accountService, final Runnable onSave,
            final Runnable onAuthFailure) {
        super(onSave, onAuthFailure);
        this.accountService = accountService;
    }

    /** Opens the dialog for creating or editing an account. */
    public void open(@Nullable final AccountResponseDto account) {
        this.removeAll();
        this.isNewAccount = account == null;
        this.currentBean = account != null ? AccountDto.from(account) : new AccountDto();
        this.binder = new Binder<>(AccountDto.class);

        final var form = this.createResponsiveFormLayout();
        final var nameField =
                this.createAndBindRequiredNameField("Account Name", AccountDto::getName, AccountDto::setName);
        form.add(nameField);

        this.initDialogLayout(account != null ? "Edit Account" : "Create Account", form);
    }

    @Override
    protected void handleSave() {
        this.executeSaveWorkflow(this.isNewAccount, "Account created successfully", "Account updated successfully",
                this.accountService::createAccount, this.accountService::updateAccount, "Account");
    }
}
