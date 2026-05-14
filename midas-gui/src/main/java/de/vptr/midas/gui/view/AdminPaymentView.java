package de.vptr.midas.gui.view;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.Route;

import de.vptr.midas.gui.component.button.CreateButton;
import de.vptr.midas.gui.component.button.DeleteButton;
import de.vptr.midas.gui.component.button.EditButton;
import de.vptr.midas.gui.component.button.RefreshButton;
import de.vptr.midas.gui.component.dialog.AdminPaymentDialog;
import de.vptr.midas.gui.component.layout.DateFilterLayout;
import de.vptr.midas.gui.component.layout.IntegerFilterLayout;
import de.vptr.midas.gui.component.layout.SearchLayout;
import de.vptr.midas.gui.dto.AccountResponseDto;
import de.vptr.midas.gui.dto.PaymentResponseDto;
import de.vptr.midas.gui.service.AccountService;
import de.vptr.midas.gui.service.PaymentService;
import de.vptr.midas.gui.service.UserService;
import de.vptr.midas.gui.util.MidasErrorHandler;
import de.vptr.midas.gui.util.NotificationUtil;
import de.vptr.midas.gui.util.UiConstants;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** Admin view for managing payments. */
@Route(value = "admin/payments", layout = AdminMainLayout.class)
public class AdminPaymentView extends AbstractAdminEntityView<PaymentResponseDto> {

    @Inject
    transient PaymentService paymentService;

    @Inject
    transient UserService userService;

    @Inject
    transient AccountService accountService;

    @Nullable
    private IntegerField limitField;

    @Nullable
    private AdminPaymentDialog paymentDialog;
    @Nullable
    private List<AccountResponseDto> availableAccounts;

    @Override
    protected void onBeforeEnterAuthenticated() {
        this.loadAccountsAsync();
        this.setupLazyDataProvider();
    }

    private void loadAccountsAsync() {
        this.loadDataAsync(() -> this.accountService.getAllAccounts(0, UiConstants.LARGE_PAGE_SIZE), accounts -> {
            this.availableAccounts = accounts;
        }, throwable -> {
            this.availableAccounts = List.of();
            MidasErrorHandler.handleInUiContext(throwable);
        }, 15);
    }

    @Override
    protected String getViewTitle() {
        return "Payments";
    }

    @Override
    protected Class<PaymentResponseDto> getEntityClass() {
        return PaymentResponseDto.class;
    }

    @Override
    protected String getEntityNameSingle() {
        return "Payment";
    }

    @Override
    protected String getSearchPlaceholder() {
        return "Search by comment or other account...";
    }

    @Override
    protected String getSearchButtonText() {
        return "Search Payments";
    }

    @Override
    protected void initAdditionalComponents() {
        this.paymentDialog =
                new AdminPaymentDialog(this.paymentService, this::setupLazyDataProvider, this.getAuthFailureRunnable());
        this.availableAccounts = List.of();
    }

    @Override
    protected void appendExtraFilters(final SearchLayout searchLayout, final DateFilterLayout dateFilterLayout,
            final IntegerFilterLayout userFilterLayout) {
        final var recentFilterLayout =
                new IntegerFilterLayout(ignore -> this.loadRecentPayments(), "How many?", "Load Recent", "100px");
        this.limitField = recentFilterLayout.getIntegerField();
        this.limitField.setMin(1);
        this.limitField.setMax(1000);
        this.limitField.setValue(10);

        searchLayout.add(recentFilterLayout, dateFilterLayout, userFilterLayout);
    }

    private void loadRecentPayments() {
        final int limit = Objects.requireNonNull(this.limitField).getValue() != null ? this.limitField.getValue() : 10;
        this.loadDataAsync(() -> this.paymentService.getRecentPayments(limit),
                payments -> Objects.requireNonNull(this.grid).setItems(payments));
    }

    @Override
    protected void setupLazyDataProvider() {
        Objects.requireNonNull(this.grid)
                .setItems(query -> this.paymentService.getAllPayments(query.getPage(), query.getPageSize()).stream());
    }

    @Override
    protected void executeSearch() {
        final String query = Objects.requireNonNull(this.searchField).getValue();
        this.executeAsyncSearch(query, q -> this.paymentService.searchPayments(q.trim()));
    }

    @Override
    protected void executeDateRangeFilter(final LocalDate startDate, final LocalDate endDate) {
        this.loadDataAsync(() -> this.paymentService.getPaymentsByDateRange(startDate, endDate),
                payments -> Objects.requireNonNull(this.grid).setItems(payments));
    }

    @Override
    protected void executeUserFilter(final Integer userId) {
        this.loadDataAsync(() -> this.paymentService.getPaymentsByUser(userId.toString()),
                payments -> Objects.requireNonNull(this.grid).setItems(payments));
    }

    @Override
    protected HorizontalLayout createButtonLayout() {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);
        layout.add(new RefreshButton(ignore -> this.setupLazyDataProvider()),
                new CreateButton(
                        ignore -> Objects.requireNonNull(this.paymentDialog).open(null,
                                this.availableAccounts != null ? this.availableAccounts : List.of()),
                        "Create Payment"));
        return layout;
    }

    @Override
    protected void configureGrid(final Grid<PaymentResponseDto> g) {
        g.addColumn((ValueProvider<PaymentResponseDto, ?>) PaymentResponseDto::getPublicId).setHeader("ID")
                .setWidth("80px").setFlexGrow(0);
        g.addColumn((ValueProvider<PaymentResponseDto, ?>) PaymentResponseDto::getUsername).setHeader("User")
                .setWidth("100px").setFlexGrow(0);
        g.addColumn((ValueProvider<PaymentResponseDto, ?>) PaymentResponseDto::getSourceAccountName).setHeader("Sender")
                .setWidth("150px").setFlexGrow(0);
        g.addColumn((ValueProvider<PaymentResponseDto, ?>) PaymentResponseDto::getTargetAccountName)
                .setHeader("Recipient").setWidth("150px").setFlexGrow(0);
        g.addColumn(payment -> payment.getAmount() != null ? payment.getAmount().toString() : "").setHeader("Amount")
                .setWidth("120px").setFlexGrow(0);
        g.addColumn((ValueProvider<PaymentResponseDto, ?>) PaymentResponseDto::getDate).setHeader("Date")
                .setWidth("120px").setFlexGrow(0);
        g.addColumn((ValueProvider<PaymentResponseDto, ?>) PaymentResponseDto::getComment).setHeader("Comment")
                .setFlexGrow(1);
        g.addColumn((ValueProvider<PaymentResponseDto, ?>) PaymentResponseDto::getCreated).setHeader("Created")
                .setWidth("120px").setFlexGrow(0);

        g.addComponentColumn(this::createActionButtons).setHeader("Actions").setWidth("150px").setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(final PaymentResponseDto payment) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var editButton = new EditButton(ignore -> Objects.requireNonNull(this.paymentDialog)
                .open(payment.toPaymentDto(), this.availableAccounts != null ? this.availableAccounts : List.of()));
        final var deleteButton = new DeleteButton(ignore -> this.handleDeletion(() -> {
            if (!this.paymentService.deletePayment(Objects.requireNonNull(payment.getPublicId()))) {
                NotificationUtil.showError("Failed to delete payment");
            }
        }));

        layout.add(editButton, deleteButton);
        return layout;
    }
}
