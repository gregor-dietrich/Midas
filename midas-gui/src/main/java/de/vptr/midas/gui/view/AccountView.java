package de.vptr.midas.gui.view;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.component.layout.DateFilterLayout;
import de.vptr.midas.gui.dto.AccountResponseDto;
import de.vptr.midas.gui.dto.PaymentResponseDto;
import de.vptr.midas.gui.service.AccountService;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.UserService;
import de.vptr.midas.gui.util.MidasErrorHandler;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** View for managing user accounts. */
@Route(value = "accounts", layout = MainLayout.class)
public class AccountView extends AbstractAsyncDataView implements BeforeEnterObserver {

    private static final Logger LOG = Logger.getLogger(AccountView.class);

    @Inject
    transient AccountService accountService;

    @Inject
    transient AuthService authService;

    @Inject
    transient UserService userService;

    @Nullable
    private Grid<AccountResponseDto> grid;
    @Nullable
    private Span totalBalanceLabel;
    @Nullable
    private DatePicker startDatePicker;
    @Nullable
    private DatePicker endDatePicker;

    private final Map<String, BigDecimal> accountBalances = new HashMap<>();
    private final Map<String, List<PaymentResponseDto>> accountPayments = new HashMap<>();
    private List<AccountResponseDto> accountsList = new ArrayList<>();
    private boolean initialized = false;

    public AccountView() {
        this.setSizeFull();
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (!this.authService.isAuthenticated()) {
            event.forwardTo(LoginView.class);
            return;
        }
        if (!this.initialized) {
            this.buildUi();
            this.initialized = true;
        }
        this.loadAccountsAsync();
    }

    private void loadAccountsAsync() {
        LOG.info("Starting async account loading for current user");

        this.loadDataAsync(() -> {
            LOG.info("Loading user accounts and their payments");
            final var accounts = this.accountService.getMyAccounts();
            final Map<String, List<PaymentResponseDto>> payments =
                    accounts.isEmpty() ? new HashMap<>() : new HashMap<>(this.accountService.getMyAccountsPayments());
            return new AccountsWithPayments(accounts, payments);
        }, result -> {
            LOG.infof("Successfully loaded %s accounts", result.accounts.size());
            this.accountPayments.clear();
            this.accountPayments.putAll(result.payments);
            this.accountsList = result.accounts;
            this.recalculateBalances();
            LOG.infof("Grid updated with %s accounts", result.accounts.size());
        }, throwable -> {
            LOG.error("Error in async account loading", throwable);
            MidasErrorHandler.handleInUiContext(throwable);
        });
    }

    private void buildUi() {
        final H2 title = new H2("My Accounts");
        title.addClassName("view-title");

        // Total balance display
        this.totalBalanceLabel = new Span("Total Balance: Loading...");
        this.totalBalanceLabel.addClassName("total-balance-label");
        this.totalBalanceLabel.getStyle().set("font-size", "1.3em");
        this.totalBalanceLabel.getStyle().set("font-weight", "bold");
        this.totalBalanceLabel.getStyle().set("margin-bottom", "1em");

        // Header layout with title and total balance
        final HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(Alignment.CENTER);

        final VerticalLayout titleSection = new VerticalLayout(title, this.totalBalanceLabel);
        titleSection.setSpacing(false);
        titleSection.setPadding(false);

        final DateFilterLayout dateFilterLayout = new DateFilterLayout(e -> this.recalculateBalances());
        this.startDatePicker = dateFilterLayout.getStartDatePicker();
        this.endDatePicker = dateFilterLayout.getEndDatePicker();

        headerLayout.add(titleSection, dateFilterLayout);

        this.createGrid();

        this.add(headerLayout, Objects.requireNonNull(this.grid));
    }

    private void createGrid() {
        this.grid = new Grid<>(AccountResponseDto.class, false);
        this.grid.setSizeFull();

        // Add columns
        this.grid.addColumn(AccountResponseDto::getName).setHeader("Account Name").setFlexGrow(2).setSortable(true);

        this.grid.addColumn(new ComponentRenderer<>(account -> {
            final Span balanceSpan = new Span();
            final BigDecimal raw = this.getAccountBalance(account.getPublicId());
            final BigDecimal effective = account.isNegative() ? raw.negate() : raw;
            balanceSpan.setText(String.format("€ %.2f", effective));
            if (effective.compareTo(BigDecimal.ZERO) > 0) {
                balanceSpan.getStyle().set("color", "var(--lumo-success-color)");
            } else if (effective.compareTo(BigDecimal.ZERO) < 0) {
                balanceSpan.getStyle().set("color", "var(--lumo-error-color)");
            }
            balanceSpan.getStyle().set("font-weight", "bold");
            return balanceSpan;
        })).setFlexGrow(1).setSortable(false);

        this.grid.addColumn(AccountResponseDto::getUserCount).setHeader("Users").setFlexGrow(0).setWidth("80px")
                .setSortable(true);

        // Add item click listener to navigate to payments view
        this.grid.addItemClickListener(event -> {
            final AccountResponseDto account = event.getItem();
            this.getUI().ifPresent(ui -> ui.navigate("accounts/" + account.getPublicId()));
        });

        // Enable pagination for performance
        this.grid.setPageSize(20);
    }

    private BigDecimal getAccountBalance(final String accountId) {
        return this.accountBalances.getOrDefault(accountId, BigDecimal.ZERO);
    }

    private void recalculateBalances() {
        final var start = Objects.requireNonNull(this.startDatePicker).getValue();
        final var end = Objects.requireNonNull(this.endDatePicker).getValue();

        this.accountBalances.clear();
        for (final var entry : this.accountPayments.entrySet()) {
            final String accountId = entry.getKey();
            final List<PaymentResponseDto> payments = entry.getValue();

            final BigDecimal balance = payments.stream()
                    .filter(p -> p.getDate() != null && !p.getDate().isBefore(start) && !p.getDate().isAfter(end))
                    .map(p -> {
                        final var amount = p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO;
                        final boolean isSender = Objects.equals(p.getSourceAccountPublicId(), accountId);
                        return isSender ? amount.negate() : amount;
                    }).reduce(BigDecimal.ZERO, BigDecimal::add);

            this.accountBalances.put(accountId, balance);
        }

        Objects.requireNonNull(this.grid).setItems(this.accountsList);
        this.updateTotalBalance();
    }

    private void updateTotalBalance() {
        if (this.totalBalanceLabel != null && this.accountBalances != null) {
            // Sum balances, subtracting those flagged as negative for the current user
            final BigDecimal totalBalance = this.accountsList.stream().reduce(BigDecimal.ZERO, (sum, account) -> {
                final BigDecimal bal = this.accountBalances.getOrDefault(account.getPublicId(), BigDecimal.ZERO);
                return account.isNegative() ? sum.subtract(bal) : sum.add(bal);
            }, BigDecimal::add);

            final String balanceText = String.format("Total Balance: € %.2f", totalBalance);
            this.totalBalanceLabel.setText(balanceText);

            // Style the total balance based on positive/negative, but not for zero
            if (totalBalance.compareTo(BigDecimal.ZERO) > 0) {
                this.totalBalanceLabel.getStyle().set("color", "var(--lumo-success-color)");
            } else if (totalBalance.compareTo(BigDecimal.ZERO) < 0) {
                this.totalBalanceLabel.getStyle().set("color", "var(--lumo-error-color)");
            } else {
                this.totalBalanceLabel.getStyle().remove("color");
            }

            LOG.infof("Updated total balance display: €%s", totalBalance);
        }
    }

    // Helper class to return both accounts and payments from the async operation
    private static class AccountsWithPayments {
        final List<AccountResponseDto> accounts;
        final Map<String, List<PaymentResponseDto>> payments;

        AccountsWithPayments(final List<AccountResponseDto> accounts,
                final Map<String, List<PaymentResponseDto>> payments) {
            this.accounts = accounts;
            this.payments = payments;
        }
    }
}
