package de.vptr.midas.gui.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import org.jboss.logging.Logger;
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
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;

import de.vptr.midas.gui.component.button.CreateButton;
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
import de.vptr.midas.gui.service.PaymentService;
import de.vptr.midas.gui.service.UserService;
import de.vptr.midas.gui.util.MidasErrorHandler;
import de.vptr.midas.gui.util.NotificationUtil;
import de.vptr.midas.gui.util.UiAsyncExecutor;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** AccountPaymentView view. */
@Route(value = "accounts/:accountId", layout = MainLayout.class)
public class AccountPaymentView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger LOG = Logger.getLogger(AccountPaymentView.class);

    // Record to hold account access verification result in a type-safe way
    private record AccountAccessResult(AccountResponseDto currentAccount, List<AccountResponseDto> userAccounts) {
    }

    private record AccountPaymentData(List<PaymentResponseDto> payments, BigDecimal balance) {
    }

    @Inject
    PaymentService paymentService;

    @Inject
    AuthService authService;

    @Inject
    UserService userService;

    @Inject
    AccountService accountService;

    @Nullable
    private Grid<PaymentResponseDto> grid;
    @Nullable
    private DatePicker startDatePicker;
    @Nullable
    private DatePicker endDatePicker;
    @Nullable
    private Span balanceLabel;

    @Nullable
    private Long accountId;
    @Nullable
    private AccountResponseDto currentAccount;
    @Nullable
    private BigDecimal accountBalance;
    @Nullable
    private List<PaymentResponseDto> allPayments; // Store all payments for client-side filtering
    @Nullable
    private List<AccountResponseDto> userAccounts; // Store user's accessible accounts

    @Nullable
    private Dialog paymentDialog;
    @Nullable
    private Binder<PaymentDto> binder;
    @Nullable
    private PaymentDto currentPayment;

    /** Initializes AccountPaymentView. */
    public AccountPaymentView() {
        this.setSizeFull();
        this.setSpacing(true);
        this.setPadding(true);
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        final RouteParameters parameters = event.getRouteParameters();
        final String accountIdStr = parameters.get("accountId").orElse(null);

        if (accountIdStr == null || accountIdStr.isEmpty()) {
            NotificationUtil.showError("Invalid account ID");
            event.forwardTo(AccountView.class);
            return;
        }

        try {
            this.accountId = Long.parseLong(accountIdStr);
        } catch (final NumberFormatException e) {
            NotificationUtil.showError("Invalid account ID format");
            event.forwardTo(AccountView.class);
            return;
        }

        // Verify user has access to this account
        this.verifyAccountAccess();
    }

    private void verifyAccountAccess() {
        UiAsyncExecutor.execute(() -> {
            final var userAccounts = this.accountService.getMyAccounts();
            final var account = userAccounts.stream().filter(acc -> acc.getId().equals(this.accountId)).findFirst()
                    .orElseThrow(() -> new ServiceException("Access denied or account not found"));

            return new AccountAccessResult(account, userAccounts);
        }, result -> {
            this.currentAccount = result.currentAccount();
            this.userAccounts = result.userAccounts();
            this.buildUi();
            this.loadPaymentsAsync();
        }, this::handleAccountAccessError);
    }

    private void loadPaymentsAsync() {
        LOG.infof("Starting async payment and balance loading for account: %s", this.accountId);

        UiAsyncExecutor.execute(() -> {
            LOG.info("Making REST call to load account payments");
            final var payments = this.accountService.getMyAccountPayments(Objects.requireNonNull(this.accountId));
            LOG.info("Loading account balance");
            final var balance = this.accountService.getMyAccountBalance(Objects.requireNonNull(this.accountId));
            return new AccountPaymentData(payments, balance);
        }, result -> {
            LOG.infof("Successfully loaded %s payments and balance: %s", result.payments().size(), result.balance());

            this.allPayments = result.payments();
            this.accountBalance = result.balance();
            Objects.requireNonNull(this.grid).setItems(result.payments());
            this.updateBalanceDisplay();
            LOG.infof("UI updated with %s payments and balance", result.payments().size());
        }, throwable -> {
            LOG.error("Error in async payment and balance loading", throwable);
            this.handleAccountDataLoadError(throwable);
        });
    }

    void handleAccountAccessError(final Throwable throwable) {
        this.handleAccountAccessError(throwable, UI.getCurrent());
    }

    void handleAccountAccessError(final Throwable throwable, final UI ui) {
        final Throwable cause = MidasErrorHandler.unwrapAsyncThrowable(throwable);
        LOG.error("Error verifying account access", cause);
        if (cause instanceof AuthenticationException) {
            NotificationUtil.showError("Session expired. Please log in again.");
            ui.navigate(LoginView.class);
            return;
        }
        if (cause instanceof TimeoutException) {
            NotificationUtil.showError("Account access check timed out. Please try again.");
            ui.navigate(AccountView.class);
            return;
        }
        NotificationUtil.showError("Access denied or account not found");
        ui.navigate(AccountView.class);
    }

    void handleAccountDataLoadError(final Throwable throwable) {
        this.handleAccountDataLoadError(throwable, UI.getCurrent());
    }

    void handleAccountDataLoadError(final Throwable throwable, final UI ui) {
        final Throwable cause = MidasErrorHandler.unwrapAsyncThrowable(throwable);
        if (cause instanceof AuthenticationException) {
            NotificationUtil.showError("Session expired. Please log in again.");
            ui.navigate(LoginView.class);
        } else if (cause instanceof TimeoutException) {
            NotificationUtil.showError("Loading account data timed out. Please try again.");
        } else if (cause instanceof ServiceException) {
            NotificationUtil.showError(cause, "Loading account data failed. Please try again.");
        } else {
            NotificationUtil.showError("Unexpected error occurred while loading account data");
        }
    }

    private void buildUi() {
        this.removeAll();
        final var acc = Objects.requireNonNull(this.currentAccount);
        final H2 title = new H2("Payments for Account: " + acc.getName());
        title.addClassName("view-title");

        // Balance display
        this.balanceLabel = new Span("Balance: Loading...");
        this.balanceLabel.addClassName("balance-label");
        this.balanceLabel.getStyle().set("font-size", "1.2em");
        this.balanceLabel.getStyle().set("font-weight", "bold");

        final HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(Alignment.CENTER);

        // Left side: title and balance
        final VerticalLayout titleSection = new VerticalLayout(title, this.balanceLabel);
        titleSection.setSpacing(false);
        titleSection.setPadding(false);

        headerLayout.add(titleSection);

        final HorizontalLayout searchLayout = this.createSearchLayout();
        final HorizontalLayout buttonLayout = this.createButtonLayout();
        this.createGrid();
        this.paymentDialog = new FormDialog();

        this.add(headerLayout, searchLayout, buttonLayout, this.grid);
    }

    private HorizontalLayout createButtonLayout() {
        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var createButton = new CreateButton(_ -> this.openPaymentDialog(null, true), "Create Payment");

        final var debitButton = new Button("", _ -> this.openPaymentDialog(null, false));
        debitButton.setIcon(LineAwesomeIcon.MINUS_SOLID.create());
        debitButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        debitButton.setTooltipText("Create Payment (as sender)");

        buttonLayout.add(createButton, debitButton);
        return buttonLayout;
    }

    private HorizontalLayout createSearchLayout() {
        final var searchLayout = new AsyncSearchLayout(this::searchPayments, () -> {
            if (this.allPayments != null) {
                Objects.requireNonNull(this.grid).setItems(this.allPayments);
            }
        }, "Search by comment or other account...", "Search Payments");

        // Date range filter
        final var dateFilterLayout = new DateFilterLayout(_ -> this.filterByDateRange());
        this.startDatePicker = dateFilterLayout.getStartDatePicker();
        this.endDatePicker = dateFilterLayout.getEndDatePicker();

        searchLayout.add(dateFilterLayout);
        return searchLayout;
    }

    private void createGrid() {
        this.grid = new Grid<>(PaymentResponseDto.class, false);
        this.grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.grid.setSizeFull();

        // Configure columns
        this.grid.addColumn((ValueProvider<PaymentResponseDto, ?>) PaymentResponseDto::getDate).setHeader("Date")
                .setWidth("120px").setFlexGrow(0);

        this.grid.addColumn(new ComponentRenderer<>(payment -> {
            final String otherAccountName;
            final Long otherAccountId;

            // Show the "other" account (not the current one)
            if (Objects.requireNonNull(this.accountId).equals(payment.getSourceAccountId())) {
                otherAccountName = payment.getTargetAccountName() != null ? payment.getTargetAccountName() : "";
                otherAccountId = payment.getTargetAccountId();
            } else {
                otherAccountName = payment.getSourceAccountName() != null ? payment.getSourceAccountName() : "";
                otherAccountId = payment.getSourceAccountId();
            }

            if (otherAccountName.isEmpty()) {
                return new Span("");
            }

            // Check if user has access to this account
            final boolean hasAccess = this.userAccounts != null
                    && this.userAccounts.stream().anyMatch(acc -> acc.getId().equals(otherAccountId));

            if (hasAccess) {
                final Button accountButton = new Button(otherAccountName);
                accountButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
                accountButton
                        .addClickListener(_ -> this.getUI().ifPresent(ui -> ui.navigate("accounts/" + otherAccountId)));
                return accountButton;
            } else {
                return new Span(otherAccountName);
            }
        })).setHeader("Other Account").setWidth("250px").setFlexGrow(0);

        this.grid.addColumn(new ComponentRenderer<>(payment -> {
            final Span amountSpan = new Span();

            if (payment.getAmount() != null) {
                String amountText;
                boolean isPositive;

                if (Objects.requireNonNull(this.accountId).equals(payment.getSourceAccountId())) {
                    // Current account is sending money - show as negative (red)
                    final var oppositeAmount = payment.getAmount().negate();
                    amountText = "€ " + oppositeAmount.toString();
                    isPositive = oppositeAmount.compareTo(BigDecimal.ZERO) > 0;
                } else {
                    // Current account is receiving money or other case - show as stored
                    amountText = "€ " + payment.getAmount().toString();
                    isPositive = payment.getAmount().compareTo(BigDecimal.ZERO) > 0;
                }

                amountSpan.setText(amountText);

                // Apply additional styling to amounts
                if (isPositive) {
                    amountSpan.getStyle().set("color", "var(--lumo-success-color)");
                } else {
                    amountSpan.getStyle().set("color", "var(--lumo-error-color)");
                }
                amountSpan.getStyle().set("font-weight", "bold");
            }

            return amountSpan;
        })).setHeader("Amount").setWidth("150px").setFlexGrow(0);

        this.grid.addColumn((ValueProvider<PaymentResponseDto, ?>) PaymentResponseDto::getComment).setHeader("Comment")
                .setFlexGrow(1);

        // Enable pagination for performance
        this.grid.setPageSize(20);

        // Add click listener for payment editing
        this.grid.addItemClickListener(event -> {
            if (event.getClickCount() == 1) {
                final PaymentResponseDto payment = event.getItem();
                this.openPaymentDialog(payment.toPaymentDto(), true);
            }
        });
    }

    private void filterByDateRange() {
        final LocalDate startDate = Objects.requireNonNull(this.startDatePicker).getValue();
        final LocalDate endDate = Objects.requireNonNull(this.endDatePicker).getValue();

        if (startDate == null || endDate == null) {
            NotificationUtil.showWarning("Please select both start and end dates");
            return;
        }

        if (startDate.isAfter(endDate)) {
            NotificationUtil.showWarning("Start date must be before end date");
            return;
        }

        if (this.allPayments == null) {
            NotificationUtil.showWarning("No payments loaded yet");
            return;
        }

        // Filter client-side from already loaded payments
        final var filteredPayments = this.allPayments.stream().filter(payment -> {
            if (payment.getDate() != null) {
                return !payment.getDate().isBefore(startDate) && !payment.getDate().isAfter(endDate);
            }
            return false;
        }).toList();

        Objects.requireNonNull(this.grid).setItems(filteredPayments);
        LOG.infof("Filtered %s payments by date range %s to %s", filteredPayments.size(), startDate, endDate);
    }

    private void searchPayments(final String query) {
        if (query == null || query.trim().isEmpty()) {
            NotificationUtil.showWarning("Please enter a search query");
            return;
        }

        if (this.allPayments == null) {
            NotificationUtil.showWarning("No payments loaded yet");
            return;
        }

        final String searchTerm = query.trim().toLowerCase();

        // Filter client-side from already loaded payments
        final var filteredPayments = this.allPayments.stream().filter(payment -> {
            // Search in username
            if (payment.getUsername() != null && payment.getUsername().toLowerCase().contains(searchTerm)) {
                return true;
            }
            // Search in amount
            if (payment.getAmount() != null && payment.getAmount().toString().contains(searchTerm)) {
                return true;
            }
            // Search in comment
            if (payment.getComment() != null && payment.getComment().toLowerCase().contains(searchTerm)) {
                return true;
            }
            // Search in account names
            if (payment.getSourceAccountName() != null
                    && payment.getSourceAccountName().toLowerCase().contains(searchTerm)) {
                return true;
            }
            if (payment.getTargetAccountName() != null
                    && payment.getTargetAccountName().toLowerCase().contains(searchTerm)) {
                return true;
            }
            return false;
        }).toList();

        Objects.requireNonNull(this.grid).setItems(filteredPayments);
        LOG.infof("Filtered %s payments by search term '%s'", filteredPayments.size(), searchTerm);
    }

    private void updateBalanceDisplay() {
        if (this.balanceLabel != null && this.accountBalance != null) {
            final String balanceText = "Balance: €" + this.accountBalance.toString();
            this.balanceLabel.setText(balanceText);

            // Style the balance based on positive/negative
            if (this.accountBalance.compareTo(BigDecimal.ZERO) >= 0) {
                this.balanceLabel.getStyle().set("color", "var(--lumo-success-color)");
            } else {
                this.balanceLabel.getStyle().set("color", "var(--lumo-error-color)");
            }
        }
    }

    private void openPaymentDialog(final PaymentDto payment, final boolean openAsRecipient) {
        Objects.requireNonNull(this.paymentDialog).removeAll();
        this.currentPayment = payment != null ? payment : new PaymentDto();

        this.binder = new Binder<>(PaymentDto.class);

        final var title = new H3(payment != null ? "Edit Payment" : "Create Payment");

        final var form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        // Form fields
        final var sourceAccountField = new ComboBox<AccountResponseDto>("Sender");
        final var targetAccountField = new ComboBox<AccountResponseDto>("Recipient");
        final var amountField = new BigDecimalField("Amount");
        final var dateField = new DatePicker("Payment Date");
        final var commentField = new TextField("Comment");
        commentField.setMaxLength(255);

        // Configure account dropdowns - only show user's accessible accounts
        sourceAccountField.setItems(this.userAccounts);
        sourceAccountField.setItemLabelGenerator(account -> account.getId() + " - " + account.getName());
        sourceAccountField.setPlaceholder("Select Sender account...");
        sourceAccountField.setInvalid(false);

        targetAccountField.setItems(this.userAccounts);
        targetAccountField.setItemLabelGenerator(account -> account.getId() + " - " + account.getName());
        targetAccountField.setPlaceholder("Select Recipient account...");
        targetAccountField.setInvalid(false);

        // Clear validation state for other fields
        amountField.setInvalid(false);
        dateField.setInvalid(false);
        commentField.setInvalid(false);

        // Add validation to prevent same account selection
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

        this.binder.forField(sourceAccountField).bind(payment1 -> this.findAccountById(payment1.getSourceAccountId()),
                (payment1, value) -> payment1.setSourceAccountId(value != null ? value.getId() : null));

        this.binder.forField(targetAccountField).bind(payment1 -> this.findAccountById(payment1.getTargetAccountId()),
                (payment1, value) -> payment1.setTargetAccountId(value != null ? value.getId() : null));

        this.binder.forField(amountField).withValidator(value -> value != null && value.compareTo(BigDecimal.ZERO) > 0,
                "Amount must be positive").bind(PaymentDto::getAmount, PaymentDto::setAmount);

        this.binder.forField(dateField).withValidator(value -> value != null, "Payment date is required")
                .bind(PaymentDto::getDate, PaymentDto::setDate);

        this.binder.forField(commentField)
                .withValidator(value -> value != null && !value.trim().isEmpty(), "Payment comment is required")
                .bind(PaymentDto::getComment, PaymentDto::setComment);

        form.add(sourceAccountField, targetAccountField, amountField, dateField, commentField);

        // Button layout
        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var saveButton = new Button("Save", _ -> this.savePayment());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        final var cancelButton = new Button("Cancel", _ -> this.paymentDialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttonLayout.add(saveButton, cancelButton);

        final var dialogLayout = new VerticalLayout(title, form, buttonLayout);
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(false);

        this.paymentDialog.add(dialogLayout);

        // Load current payment data
        Objects.requireNonNull(this.binder).readBean(this.currentPayment);

        // Pre-select current account and today's date for new payments
        if (payment == null) {
            if (openAsRecipient) {
                targetAccountField.setValue(this.currentAccount);
            } else {
                sourceAccountField.setValue(this.currentAccount);
            }
            dateField.setValue(LocalDate.now());
        }

        this.paymentDialog.open();
    }

    private void savePayment() {
        try {
            // Validate form fields first
            if (!Objects.requireNonNull(this.binder).validate().isOk()) {
                NotificationUtil.showError("Please check the form for errors");
                return;
            }

            this.binder.writeBean(this.currentPayment);

            // Custom validation: at least one account must be selected
            if (this.currentPayment.getSourceAccountId() == null && this.currentPayment.getTargetAccountId() == null) {
                NotificationUtil.showError("At least one account (source or target) must be selected");
                return;
            }

            if (this.currentPayment.getId() == null) {
                if (this.currentPayment.getUserId() == null) {
                    final var currentUser = this.userService.getCurrentUser();
                    if (currentUser.isPresent()) {
                        this.currentPayment.setUserId(currentUser.get().getId());
                    } else {
                        NotificationUtil.showError("Unable to determine current user");
                        return;
                    }
                }
                this.accountService.createMyAccountPayment(this.accountId, this.currentPayment);
                NotificationUtil.showSuccess("Payment created successfully");
            } else {
                this.accountService.updateMyAccountPayment(this.accountId, this.currentPayment.getId(),
                        this.currentPayment);
                NotificationUtil.showSuccess("Payment updated successfully");
            }

            this.paymentDialog.close();
            this.loadPaymentsAsync();

        } catch (final ValidationException e) {
            NotificationUtil.showError("Please check the form for errors");
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        } catch (final ServiceException e) {
            NotificationUtil.showError(e, "Saving payment failed. Please try again.");
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error while saving payment", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private AccountResponseDto findAccountById(final Long accountId) {
        if (accountId == null || this.userAccounts == null) {
            return null;
        }
        return this.userAccounts.stream().filter(account -> account.getId().equals(accountId)).findFirst().orElse(null);
    }
}
