package de.vptr.midas.gui.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

import org.vaadin.lineawesome.LineAwesomeIcon;

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
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.component.button.CreateButton;
import de.vptr.midas.gui.component.dialog.FormDialog;
import de.vptr.midas.gui.component.layout.AsyncSearchLayout;
import de.vptr.midas.gui.component.layout.DateFilterLayout;
import de.vptr.midas.gui.dto.AccountResponseDto;
import de.vptr.midas.gui.dto.PaymentDto;
import de.vptr.midas.gui.dto.PaymentResponseDto;
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

    // Record to hold account access verification result in a type-safe way
    private record AccountAccessResult(@Nullable AccountResponseDto currentAccount,
            List<AccountResponseDto> userAccounts) {
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

        try {
            this.accountId = Long.parseLong(accountIdStr);
        } catch (final NumberFormatException e) {
            NotificationUtil.showError("Invalid Account ID");
            event.forwardTo(HomeView.class);
            return;
        }

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
                    userAccounts.stream().filter(acc -> acc.getId().equals(aid)).findFirst().orElse(null);
            return new AccountAccessResult(currentAccount, userAccounts);
        }, result -> {
            if (result.currentAccount() == null) {
                NotificationUtil.showError("Account not found or access denied");
                this.getUI().ifPresent(ui -> ui.navigate(HomeView.class));
                return;
            }
            this.currentAccount = result.currentAccount();
            this.userAccounts = result.userAccounts();
            this.loadPaymentsAndBalance();
        });
    }

    private void loadPaymentsAndBalance() {
        final var aid = Objects.requireNonNull(this.accountId);
        this.loadDataAsync(() -> {
            final var payments = this.paymentService.getPaymentsByAccount(aid);
            final var balance = this.accountService.getAccountBalance(aid);
            return new AccountPaymentData(payments, balance);
        }, data -> {
            this.allPayments = data.payments();
            this.accountBalance = data.balance();
            this.updateUiWithData();
        });
    }

    private void updateUiWithData() {
        Objects.requireNonNull(this.grid).setItems(this.allPayments != null ? this.allPayments : List.of());
        Objects.requireNonNull(this.balanceLabel).setText(
                "Current Balance: " + (this.accountBalance != null ? this.accountBalance : BigDecimal.ZERO) + " EUR");
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
            if (query == null || query.trim().isEmpty()) {
                this.updateUiWithData();
                return;
            }
            this.filterPayments(query.trim());
        }, "Search by comment or recipient...", "Search Payments");

        final var dateFilterLayout = new DateFilterLayout(_ -> this.filterByDateRange());
        this.startDatePicker = dateFilterLayout.getStartDatePicker();
        this.endDatePicker = dateFilterLayout.getEndDatePicker();

        final var layout = new HorizontalLayout(searchLayout, dateFilterLayout);
        layout.setAlignItems(Alignment.END);
        return layout;
    }

    private HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var sendButton = new CreateButton(_ -> this.openPaymentDialog(null, false), "Send Payment");
        final var receiveButton = new CreateButton(_ -> this.openPaymentDialog(null, true), "Receive Payment");
        receiveButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        final var refreshButton =
                new Button(LineAwesomeIcon.SYNC_ALT_SOLID.create(), _ -> this.loadPaymentsAndBalance());
        refreshButton.setTooltipText("Refresh Data");

        layout.add(sendButton, receiveButton, refreshButton);
        return layout;
    }

    private void createGrid() {
        this.grid = new Grid<>(PaymentResponseDto.class, false);
        this.grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.grid.setSizeFull();

        this.grid.addColumn((ValueProvider<PaymentResponseDto, ?>) PaymentResponseDto::getId).setHeader("ID")
                .setWidth("80px").setFlexGrow(0);
        this.grid.addColumn((ValueProvider<PaymentResponseDto, ?>) PaymentResponseDto::getDate).setHeader("Date")
                .setWidth("120px").setFlexGrow(0);

        this.grid.addColumn(new ComponentRenderer<>(payment -> {
            final var isSender = Objects.equals(payment.getSourceAccountId(), this.accountId);
            final var span = new Span(isSender ? "Sent" : "Received");
            span.getElement().getThemeList().add(isSender ? "badge error" : "badge success");
            return span;
        })).setHeader("Type").setWidth("100px").setFlexGrow(0);

        this.grid.addColumn(payment -> {
            final var isSender = Objects.equals(payment.getSourceAccountId(), this.accountId);
            return isSender ? payment.getTargetAccountName() : payment.getSourceAccountName();
        }).setHeader("Counterparty").setFlexGrow(1);

        this.grid.addColumn(new ComponentRenderer<>(payment -> {
            final var isSender = Objects.equals(payment.getSourceAccountId(), this.accountId);
            final var amount = payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
            final var span = new Span((isSender ? "-" : "+") + amount + " EUR");
            span.getStyle().set("color", isSender ? "var(--lumo-error-text-color)" : "var(--lumo-success-text-color)");
            span.getStyle().set("font-weight", "bold");
            return span;
        })).setHeader("Amount").setWidth("150px").setFlexGrow(0);

        this.grid.addColumn(PaymentResponseDto::getComment).setHeader("Comment").setFlexGrow(2);

        this.grid.addComponentColumn(payment -> {
            final var editButton = new Button(LineAwesomeIcon.EDIT.create(),
                    _ -> this.openPaymentDialog(payment.toPaymentDto(), false));
            editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            return editButton;
        }).setHeader("Actions").setWidth("100px").setFlexGrow(0);
    }

    private void filterPayments(final String query) {
        if (this.allPayments == null) {
            return;
        }
        final var filtered = this.allPayments.stream()
                .filter(p -> (p.getComment() != null && p.getComment().toLowerCase().contains(query.toLowerCase()))
                        || (p.getSourceAccountName() != null
                                && p.getSourceAccountName().toLowerCase().contains(query.toLowerCase()))
                        || (p.getTargetAccountName() != null
                                && p.getTargetAccountName().toLowerCase().contains(query.toLowerCase())))
                .toList();
        Objects.requireNonNull(this.grid).setItems(filtered);
    }

    private void filterByDateRange() {
        if (this.allPayments == null) {
            return;
        }
        final var start = Objects.requireNonNull(this.startDatePicker).getValue();
        final var end = Objects.requireNonNull(this.endDatePicker).getValue();

        if (start == null || end == null) {
            return;
        }

        final var filtered = this.allPayments.stream()
                .filter(p -> p.getDate() != null && !p.getDate().isBefore(start) && !p.getDate().isAfter(end)).toList();
        Objects.requireNonNull(this.grid).setItems(filtered);
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

        this.binder.forField(sourceAccountField).bind(p -> this.findAccountById(p.getSourceAccountId()),
                (p, v) -> p.setSourceAccountId(v != null ? v.getId() : null));
        this.binder.forField(targetAccountField).bind(p -> this.findAccountById(p.getTargetAccountId()),
                (p, v) -> p.setTargetAccountId(v != null ? v.getId() : null));

        this.binder.forField(amountField)
                .withValidator(v -> v != null && v.compareTo(BigDecimal.ZERO) > 0, "Amount must be positive")
                .bind(PaymentDto::getAmount, PaymentDto::setAmount);
        this.binder.forField(dateField).withValidator(v -> v != null, "Date is required").bind(PaymentDto::getDate,
                PaymentDto::setDate);
        this.binder.forField(commentField).withValidator(v -> v != null && !v.trim().isEmpty(), "Comment is required")
                .bind(PaymentDto::getComment, PaymentDto::setComment);

        form.add(sourceAccountField, targetAccountField, amountField, dateField, commentField);

        final var buttonLayout = new HorizontalLayout();
        final var saveButton = new Button("Save", _ -> this.savePayment());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        final var cancelButton = new Button("Cancel", _ -> pd.close());
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
    private AccountResponseDto findAccountById(@Nullable final Long id) {
        if (id == null || this.userAccounts == null) {
            return null;
        }
        return this.userAccounts.stream().filter(acc -> acc.getId().equals(id)).findFirst().orElse(null);
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

            if (cp.getSourceAccountId() == null && cp.getTargetAccountId() == null) {
                NotificationUtil.showError("At least one account must be selected");
                return;
            }

            final var aid = Objects.requireNonNull(this.accountId);
            if (cp.getId() == null) {
                if (cp.getUserId() == null) {
                    this.userService.getCurrentUser().ifPresent(u -> cp.setUserId(u.getId()));
                }
                this.accountService.createMyAccountPayment(aid, cp);
                NotificationUtil.showSuccess("Payment created successfully");
            } else {
                this.accountService.updateMyAccountPayment(aid, Objects.requireNonNull(cp.getId()), cp);
                NotificationUtil.showSuccess("Payment updated successfully");
            }

            Objects.requireNonNull(this.paymentDialog).close();
            this.loadPaymentsAndBalance();
        } catch (final Exception e) {
            MidasErrorHandler.handleInUiContext(e);
        }
    }

    private <T> void loadDataAsync(final java.util.function.Supplier<T> supplier,
            final java.util.function.Consumer<T> consumer) {
        UiAsyncExecutor.execute(supplier, consumer, MidasErrorHandler::handleInUiContext);
    }
}
