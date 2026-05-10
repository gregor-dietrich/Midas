package de.vptr.midas.gui.view;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.dto.AccountResponseDto;
import de.vptr.midas.gui.service.AccountService;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.UserService;
import de.vptr.midas.gui.util.MidasErrorHandler;
import jakarta.inject.Inject;

/** View for managing user accounts. */
@Route(value = "accounts", layout = MainLayout.class)
public class AccountView extends AbstractAsyncDataView implements BeforeEnterObserver {

    private static final Logger LOG = Logger.getLogger(AccountView.class);

    @Inject
    AccountService accountService;

    @Inject
    AuthService authService;

    @Inject
    UserService userService;

    private Grid<AccountResponseDto> grid;
    private Span totalBalanceLabel;
    private Map<Long, BigDecimal> accountBalances = new HashMap<>();
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
            LOG.info("Making optimized REST call to load user accounts with balances");
            final var accounts = this.accountService.getMyAccountsWithBalances();
            final Map<Long, BigDecimal> balances = accounts.stream().collect(Collectors.toMap(AccountResponseDto::getId,
                    account -> account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO));
            return new AccountsWithBalances(accounts, balances);
        }, result -> {
            LOG.infof("Successfully loaded %s accounts", result.accounts.size());
            this.accountBalances = result.balances;
            this.accountsList = result.accounts;
            this.grid.setItems(result.accounts);
            this.updateTotalBalance();
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

        headerLayout.add(titleSection);

        this.createGrid();

        this.add(headerLayout, this.grid);
    }

    private void createGrid() {
        this.grid = new Grid<>(AccountResponseDto.class, false);
        this.grid.setSizeFull();

        // Add columns
        this.grid.addColumn(AccountResponseDto::getName).setHeader("Account Name").setFlexGrow(2).setSortable(true);

        this.grid.addColumn(new ComponentRenderer<>(account -> {
            final Span balanceSpan = new Span();
            final BigDecimal raw = this.getAccountBalance(account.getId());
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
            this.getUI().ifPresent(ui -> ui.navigate("accounts/" + account.getId()));
        });

        // Enable pagination for performance
        this.grid.setPageSize(20);
    }

    private BigDecimal getAccountBalance(final Long accountId) {
        return this.accountBalances.getOrDefault(accountId, BigDecimal.ZERO);
    }

    private void updateTotalBalance() {
        if (this.totalBalanceLabel != null && this.accountBalances != null) {
            // Sum balances, subtracting those flagged as negative for the current user
            final BigDecimal totalBalance = this.accountsList.stream().reduce(BigDecimal.ZERO, (sum, account) -> {
                final BigDecimal bal = this.accountBalances.getOrDefault(account.getId(), BigDecimal.ZERO);
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

    // Helper class to return both accounts and balances from the async operation
    private static class AccountsWithBalances {
        final List<AccountResponseDto> accounts;
        final Map<Long, BigDecimal> balances;

        AccountsWithBalances(final List<AccountResponseDto> accounts, final Map<Long, BigDecimal> balances) {
            this.accounts = accounts;
            this.balances = balances;
        }
    }
}
