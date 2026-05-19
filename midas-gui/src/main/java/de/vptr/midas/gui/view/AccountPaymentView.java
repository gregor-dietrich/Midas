package de.vptr.midas.gui.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LocalDateRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.component.dialog.FormDialog;
import de.vptr.midas.gui.component.layout.AsyncSearchLayout;
import de.vptr.midas.gui.component.layout.DateFilterLayout;
import de.vptr.midas.gui.dto.AccountResponseDto;
import de.vptr.midas.gui.dto.PaymentDto;
import de.vptr.midas.gui.dto.PaymentResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.AccountService;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.UserService;
import de.vptr.midas.gui.util.MidasErrorHandler;
import de.vptr.midas.gui.util.NotificationUtil;
import de.vptr.midas.gui.util.UiAsyncExecutor;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** AccountPaymentView view. */
@Route(value = "accounts/:accountId", layout = MainLayout.class)
public class AccountPaymentView extends VerticalLayout implements BeforeEnterObserver {

    // Record to hold account access verification result in a type-safe way
    private record AccountAccessResult(@Nullable AccountResponseDto currentAccount,
            List<AccountResponseDto> userAccounts) {
    }

    @Inject
    transient AuthService authService;

    @Inject
    transient UserService userService;

    @Inject
    transient AccountService accountService;

    @Nullable
    private Grid<PaymentResponseDto> grid;
    @Nullable
    private DatePicker startDatePicker;
    @Nullable
    private DatePicker endDatePicker;
    @Nullable
    private Span balanceLabel;

    @Nullable
    private String accountId;
    @Nullable
    private transient AccountResponseDto currentAccount;
    @Nullable
    private String searchQuery; // Current free-text filter, applied server-side
    @Nullable
    private List<AccountResponseDto> userAccounts; // Store user's accessible accounts
    private final Set<String> accessibleAccountIds = new HashSet<>();

    @Nullable
    private Dialog paymentDialog;
    @Nullable
    private Binder<PaymentDto> binder;
    @Nullable
    private transient PaymentDto currentPayment;

    @Nullable
    private Button sendButton;
    @Nullable
    private Button receiveButton;
    @Nullable
    private Grid.Column<PaymentResponseDto> actionsColumn;

    private boolean initialized = false;

    /** Initializes AccountPaymentView. */
    public AccountPaymentView() {
        this.setSizeFull();
        this.setPadding(true);
        this.setSpacing(true);
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (!this.authService.isAuthenticated()) {
            event.forwardTo(LoginView.class);
            return;
        }

        final var routeParams = event.getRouteParameters();
        final var accountIdStr = routeParams.get("accountId").orElse(null);

        if (accountIdStr == null) {
            NotificationUtil.showError("Account ID is missing");
            event.forwardTo(HomeView.class);
            return;
        }

        this.accountId = accountIdStr;

        if (!this.initialized) {
            this.buildUi();
            this.initialized = true;
        }

        this.verifyAccountAccessAndLoadData();
    }

    private void verifyAccountAccessAndLoadData() {
        final var aid = Objects.requireNonNull(this.accountId);
        this.loadDataAsync(() -> {
            final var userAccounts = this.accountService.getMyAccounts();
            final var currentAccount =
                    userAccounts.stream().filter(acc -> acc.getPublicId().equals(aid)).findFirst().orElse(null);
            return new AccountAccessResult(currentAccount, userAccounts);
        }, result -> {
            if (result.currentAccount() == null) {
                NotificationUtil.showError("Account not found or access denied");
                this.getUI().ifPresent(ui -> ui.navigate(HomeView.class));
                return;
            }
            this.currentAccount = result.currentAccount();
            this.userAccounts = result.userAccounts();
            this.accessibleAccountIds.clear();
            if (this.userAccounts != null) {
                this.userAccounts.forEach(acc -> this.accessibleAccountIds.add(acc.getPublicId()));
            }
            this.applyManagePermissions();
            this.setupLazyDataProvider();
            this.reloadBalance();
        });
    }

    /** Updates the management-only UI affordances based on the current account's manage permission. */
    private void applyManagePermissions() {
        final boolean canManage = this.currentAccount != null && this.currentAccount.isCanManage();
        if (this.sendButton != null) {
            this.sendButton.setVisible(canManage);
        }
        if (this.receiveButton != null) {
            this.receiveButton.setVisible(canManage);
        }
        if (this.actionsColumn != null) {
            this.actionsColumn.setVisible(canManage);
        }
    }

    /**
     * Wires the grid to a lazy, server-paginated data provider. Date-range and free-text filtering are passed to the
     * API on every page request, so the full payment set is never loaded into the GUI.
     */
    private void setupLazyDataProvider() {
        final var aid = Objects.requireNonNull(this.accountId);
        Objects.requireNonNull(this.grid)
                .setItems(query -> this.accountService.getMyAccountPayments(aid, this.selectedStartDate(),
                        this.selectedEndDate(), this.searchQuery, query.getPage(), query.getPageSize()).stream());
    }

    /** Re-fetches the date-ranged balance from the server (computed server-side, no payment rows transferred). */
    private void reloadBalance() {
        final var aid = Objects.requireNonNull(this.accountId);
        this.loadDataAsync(
                () -> this.accountService.getMyAccountBalance(aid, this.selectedStartDate(), this.selectedEndDate()),
                this::updateBalanceLabel);
    }

    /** Triggers a server round-trip to reload the current page of payments with the active filters. */
    private void refreshPayments() {
        final var g = this.grid;
        if (g != null) {
            g.getDataProvider().refreshAll();
        }
    }

    /** Reloads both the server-computed balance and the current page of payments. */
    private void reloadBalanceAndPayments() {
        this.reloadBalance();
        this.refreshPayments();
    }

    @Nullable
    private LocalDate selectedStartDate() {
        return this.startDatePicker != null ? this.startDatePicker.getValue() : null;
    }

    @Nullable
    private LocalDate selectedEndDate() {
        return this.endDatePicker != null ? this.endDatePicker.getValue() : null;
    }

    private void updateBalanceLabel(final BigDecimal balance) {
        final var label = Objects.requireNonNull(this.balanceLabel);
        label.setText("Current Balance: " + balance + " EUR");
        if (balance.compareTo(BigDecimal.ZERO) > 0) {
            label.getStyle().set("color", "var(--lumo-success-text-color)");
        } else if (balance.compareTo(BigDecimal.ZERO) < 0) {
            label.getStyle().set("color", "var(--lumo-error-text-color)");
        } else {
            label.getStyle().remove("color");
        }
    }

    private void buildUi() {
        this.removeAll();

        final var header = new H2("Account Payments");
        this.balanceLabel = new Span("Current Balance: 0.00 EUR");
        this.balanceLabel.getStyle().set("font-weight", "bold");
        this.balanceLabel.getStyle().set("font-size", "var(--lumo-font-size-l)");

        final var searchLayout = this.createSearchLayout();
        final var buttonLayout = this.createButtonLayout();
        this.createGrid();

        this.paymentDialog = new FormDialog();

        this.add(header, this.balanceLabel, searchLayout, buttonLayout, Objects.requireNonNull(this.grid));
    }

    private HorizontalLayout createSearchLayout() {
        final var searchLayout = new AsyncSearchLayout(query -> {
            this.searchQuery = (query == null || query.trim().isEmpty()) ? null : query.trim();
            this.refreshPayments();
        }, "Search by comment or recipient...", "Search Payments");

        final var dateFilterLayout = new DateFilterLayout(e -> this.reloadBalanceAndPayments());
        this.startDatePicker = dateFilterLayout.getStartDatePicker();
        this.endDatePicker = dateFilterLayout.getEndDatePicker();

        final var layout = new HorizontalLayout(searchLayout, dateFilterLayout);
        layout.setAlignItems(Alignment.END);
        return layout;
    }

    private HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        this.sendButton = new Button(LineAwesomeIcon.ARROW_UP_SOLID.create(), e -> this.openPaymentDialog(null, false));
        this.sendButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        this.sendButton.setVisible(false);

        this.receiveButton =
                new Button(LineAwesomeIcon.ARROW_DOWN_SOLID.create(), e -> this.openPaymentDialog(null, true));
        this.receiveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        this.receiveButton.setVisible(false);

        final var refreshButton =
                new Button(LineAwesomeIcon.SYNC_ALT_SOLID.create(), e -> this.reloadBalanceAndPayments());
        refreshButton.setTooltipText("Refresh Data");

        layout.add(refreshButton, this.sendButton, this.receiveButton);
        return layout;
    }

    private void createGrid() {
        this.grid = new Grid<>(PaymentResponseDto.class, false);
        this.grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.grid.setSizeFull();

        this.grid.addColumn(new LocalDateRenderer<>(PaymentResponseDto::getDate)).setHeader("Date").setAutoWidth(true)
                .setFlexGrow(0);

        this.grid.addColumn(payment -> {
            final var isSender = Objects.equals(payment.getSourceAccountPublicId(), this.accountId);
            return isSender ? payment.getTargetAccountName() : payment.getSourceAccountName();
        }).setHeader("Counterparty").setAutoWidth(true).setFlexGrow(0);

        this.grid.addColumn(new ComponentRenderer<>(payment -> {
            final var isSender = Objects.equals(payment.getSourceAccountPublicId(), this.accountId);
            final var amount = payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
            final var span = new Span((isSender ? "-" : "+") + amount + " EUR");
            span.getStyle().set("color", isSender ? "var(--lumo-error-text-color)" : "var(--lumo-success-text-color)");
            span.getStyle().set("font-weight", "bold");
            return span;
        })).setHeader("Amount").setWidth("150px").setFlexGrow(0);

        this.grid.addColumn(PaymentResponseDto::getComment).setHeader("Comment").setFlexGrow(2);

        this.actionsColumn = this.grid.addComponentColumn(payment -> {
            final var editButton = new Button(LineAwesomeIcon.EDIT.create(),
                    e -> this.openPaymentDialog(payment.toPaymentDto(), false));
            editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            final var deleteButton =
                    new Button(LineAwesomeIcon.TRASH_ALT_SOLID.create(), e -> this.deletePayment(payment));
            deleteButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

            // Counterparty logic: hide if payment has counterparty and user is not
            // associated with it
            final boolean hasCounterparty =
                    payment.getSourceAccountPublicId() != null && payment.getTargetAccountPublicId() != null;
            final boolean associatedWithBoth =
                    hasCounterparty && this.accessibleAccountIds.contains(payment.getSourceAccountPublicId())
                            && this.accessibleAccountIds.contains(payment.getTargetAccountPublicId());
            final boolean canEditOrDelete = !hasCounterparty || associatedWithBoth;

            editButton.setVisible(canEditOrDelete);
            deleteButton.setVisible(canEditOrDelete);

            final var layout = new HorizontalLayout(editButton, deleteButton);
            layout.setSpacing(false);
            return layout;
        }).setHeader("Actions").setAutoWidth(true).setFlexGrow(0);
        this.actionsColumn.setVisible(false);
    }

    private void openPaymentDialog(@Nullable final PaymentDto payment, final boolean openAsRecipient) {
        final var pd = Objects.requireNonNull(this.paymentDialog);
        pd.removeAll();
        this.currentPayment = payment != null ? payment : new PaymentDto();
        this.binder = new Binder<>(PaymentDto.class);

        final var title =
                new H3(payment != null ? "Edit Payment" : (openAsRecipient ? "Receive Payment" : "Send Payment"));

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        final var sourceAccountField = new ComboBox<AccountResponseDto>("Source Account");
        sourceAccountField.setItems(this.userAccounts != null ? this.userAccounts : List.of());
        sourceAccountField.setItemLabelGenerator(AccountResponseDto::getName);
        sourceAccountField.setWidthFull();

        final var targetAccountField = new ComboBox<AccountResponseDto>("Recipient Account");
        targetAccountField.setItems(this.userAccounts != null ? this.userAccounts : List.of());
        targetAccountField.setItemLabelGenerator(AccountResponseDto::getName);
        targetAccountField.setWidthFull();

        final var amountField = new BigDecimalField("Amount (EUR)");
        amountField.setWidthFull();

        final var dateField = new DatePicker("Date");
        dateField.setWidthFull();

        final var commentField = new TextField("Comment");
        commentField.setWidthFull();

        // Validation for account selection
        sourceAccountField.addValueChangeListener(e -> {
            if (e.getValue() != null && e.getValue().equals(targetAccountField.getValue())) {
                NotificationUtil.showWarning("Sender and Recipient accounts cannot be the same");
                sourceAccountField.setValue(null);
            }
        });

        targetAccountField.addValueChangeListener(e -> {
            if (e.getValue() != null && e.getValue().equals(sourceAccountField.getValue())) {
                NotificationUtil.showWarning("Sender and Recipient accounts cannot be the same");
                targetAccountField.setValue(null);
            }
        });

        this.binder.forField(sourceAccountField).bind(p -> this.findAccountById(p.getSourceAccountPublicId()),
                (p, v) -> p.setSourceAccountPublicId(v != null ? v.getPublicId() : null));
        this.binder.forField(targetAccountField).bind(p -> this.findAccountById(p.getTargetAccountPublicId()),
                (p, v) -> p.setTargetAccountPublicId(v != null ? v.getPublicId() : null));

        this.binder.forField(amountField)
                .withValidator(v -> v != null && v.compareTo(BigDecimal.ZERO) > 0, "Amount must be positive")
                .bind(PaymentDto::getAmount, PaymentDto::setAmount);
        this.binder.forField(dateField).withValidator(v -> v != null, "Date is required").bind(PaymentDto::getDate,
                PaymentDto::setDate);
        this.binder.forField(commentField).withValidator(v -> v != null && !v.trim().isEmpty(), "Comment is required")
                .bind(PaymentDto::getComment, PaymentDto::setComment);

        form.add(sourceAccountField, targetAccountField, amountField, dateField, commentField);

        final var buttonLayout = new HorizontalLayout();
        final var saveButton = new Button("Save", e -> this.savePayment());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        final var cancelButton = new Button("Cancel", e -> pd.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        buttonLayout.add(saveButton, cancelButton);

        pd.add(new VerticalLayout(title, form, buttonLayout));

        this.binder.readBean(this.currentPayment);

        if (payment == null) {
            if (openAsRecipient) {
                targetAccountField.setValue(this.currentAccount);
            } else {
                sourceAccountField.setValue(this.currentAccount);
            }
            dateField.setValue(LocalDate.now(ZoneId.systemDefault()));
        }

        pd.open();
    }

    @Nullable
    private AccountResponseDto findAccountById(@Nullable final String id) {
        if (id == null || this.userAccounts == null) {
            return null;
        }
        return this.userAccounts.stream().filter(acc -> acc.getPublicId().equals(id)).findFirst().orElse(null);
    }

    private void savePayment() {
        try {
            final var b = Objects.requireNonNull(this.binder);
            final var cp = Objects.requireNonNull(this.currentPayment);
            if (!b.validate().isOk()) {
                NotificationUtil.showError("Please check the form for errors");
                return;
            }
            b.writeBean(cp);

            if (cp.getSourceAccountPublicId() == null && cp.getTargetAccountPublicId() == null) {
                NotificationUtil.showError("At least one account must be selected");
                return;
            }

            final var aid = Objects.requireNonNull(this.accountId);
            if (cp.getPublicId() == null) {
                this.accountService.createMyAccountPayment(aid, cp);
                NotificationUtil.showSuccess("Payment created successfully");
            } else {
                this.accountService.updateMyAccountPayment(aid, Objects.requireNonNull(cp.getPublicId()), cp);
                NotificationUtil.showSuccess("Payment updated successfully");
            }

            Objects.requireNonNull(this.paymentDialog).close();
            this.reloadBalanceAndPayments();
        } catch (final Exception e) {
            MidasErrorHandler.handleInUiContext(e);
        }
    }

    private void deletePayment(final PaymentResponseDto payment) {
        final var pid = payment.getPublicId();
        final var aid = Objects.requireNonNull(this.accountId);
        if (pid == null) {
            return;
        }

        final var dialog = new Dialog();
        dialog.add(
                new VerticalLayout(new H3("Confirm Delete"), new Span("Are you sure you want to delete this payment?"),
                        new HorizontalLayout(new Button("Delete", e -> {
                            this.executeWithHandling(() -> {
                                if (this.accountService.deleteMyAccountPayment(aid, pid)) {
                                    NotificationUtil.showSuccess("Payment deleted successfully");
                                    this.reloadBalanceAndPayments();
                                } else {
                                    NotificationUtil.showError("Failed to delete payment");
                                }
                            }, "Failed to delete payment");
                            dialog.close();
                        }), new Button("Cancel", e -> dialog.close()))));
        dialog.open();
    }

    private void executeWithHandling(final Runnable runnable, final String errorMessage) {
        try {
            runnable.run();
        } catch (final Exception e) {
            NotificationUtil.showError(errorMessage);
            MidasErrorHandler.handleInUiContext(e);
        }
    }

    private <T> void loadDataAsync(final Supplier<T> supplier, final Consumer<T> consumer) {
        UiAsyncExecutor.execute(supplier, consumer, MidasErrorHandler::handleInUiContext);
    }

    /** Handles account access verification errors. */
    void handleAccountAccessError(final CompletionException e, final UI ui) {
        final var cause = MidasErrorHandler.unwrapAsyncThrowable(e);
        if (cause instanceof AuthenticationException) {
            NotificationUtil.showError("Session expired. Please log in again.");
            ui.navigate(LoginView.class);
        } else {
            NotificationUtil.showError("Access denied or account not found");
            ui.navigate(AccountView.class);
        }
    }

    /** Handles account data loading errors. */
    @SuppressWarnings("PatternMatchingInstanceof")
    void handleAccountDataLoadError(final CompletionException e, final UI ui) {
        final var cause = MidasErrorHandler.unwrapAsyncThrowable(e);
        if (cause instanceof AuthenticationException) {
            NotificationUtil.showError("Session expired. Please log in again.");
            ui.navigate(LoginView.class);
        } else if (cause instanceof TimeoutException) {
            NotificationUtil.showError("Loading account data timed out. Please try again.");
        } else if (cause instanceof ServiceException) {
            NotificationUtil.showError((ServiceException) cause, "Loading account data failed. Please try again.");
        }
    }
}
