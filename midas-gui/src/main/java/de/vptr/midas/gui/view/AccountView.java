package de.vptr.midas.gui.view;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
import de.vptr.midas.gui.service.AccountService;
import de.vptr.midas.gui.service.AuthService;
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

    @Nullable
    private Grid<AccountResponseDto> grid;
    @Nullable
    private Span totalBalanceLabel;
    @Nullable
    private DatePicker startDatePicker;
    @Nullable
    private DatePicker endDatePicker;

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

    /**
     * Loads the current user's accounts with server-computed balances for the selected date range. The balance and its
     * date-range aggregation are performed entirely server-side; no payment rows are transferred to the GUI.
     */
    private void loadAccountsAsync() {
        final var start = this.startDatePicker != null ? this.startDatePicker.getValue() : null;
        final var end = this.endDatePicker != null ? this.endDatePicker.getValue() : null;
        LOG.infof("Loading accounts with balances for current user (range %s..%s)", start, end);

        this.loadDataAsync(() -> this.accountService.getMyAccountsWithBalances(start, end), accounts -> {
            LOG.infof("Successfully loaded %s accounts with balances", accounts.size());
            this.accountsList = accounts;
            Objects.requireNonNull(this.grid).setItems(this.accountsList);
            this.updateTotalBalance();
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

        final DateFilterLayout dateFilterLayout = new DateFilterLayout(e -> this.loadAccountsAsync());
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
            final BigDecimal effective = this.effectiveBalance(account);
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

    /** Returns the account balance as the current user sees it, applying the per-user negative-account flag. */
    private BigDecimal effectiveBalance(final AccountResponseDto account) {
        final BigDecimal raw = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
        return account.isNegative() ? raw.negate() : raw;
    }

    private void updateTotalBalance() {
        if (this.totalBalanceLabel == null) {
            return;
        }
        final BigDecimal totalBalance =
                this.accountsList.stream().map(this::effectiveBalance).reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalBalanceLabel.setText(String.format("Total Balance: € %.2f", totalBalance));

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
