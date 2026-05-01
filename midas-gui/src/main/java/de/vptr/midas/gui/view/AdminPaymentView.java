package de.vptr.midas.gui.view;

import java.time.LocalDate;
import java.util.List;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
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
import de.vptr.midas.gui.util.UiConstants;
import jakarta.inject.Inject;

/** Admin view for managing payments. */
@Route(value = "admin/payments", layout = AdminMainLayout.class)
public class AdminPaymentView extends AbstractAsyncDataView implements BeforeEnterObserver {

    private static final Logger LOG = Logger.getLogger(AdminPaymentView.class);

    @Inject
    PaymentService paymentService;

    @Inject
    AuthService authService;

    @Inject
    UserService userService;

    @Inject
    AccountService accountService;

    private Grid<PaymentResponseDto> grid;
    private TextField searchField;
    private Button searchButton;
    private IntegerField limitField;
    private DatePicker startDatePicker;
    private DatePicker endDatePicker;
    private IntegerField userIdField;

    private AdminPaymentDialog paymentDialog;
    private List<AccountResponseDto> availableAccounts;

    /** Initializes AdminPaymentView. */
    public AdminPaymentView() {
        this.setSizeFull();
        this.setSpacing(true);
        this.setPadding(true);
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        LOG.info("AdminPaymentView.beforeEnter - Starting view initialization");
        this.buildUi();
        this.loadAccountsAsync();
        this.setupLazyDataProvider();
        LOG.info("AdminPaymentView.beforeEnter - View initialization completed");
    }

    private void setupLazyDataProvider() {
        this.grid.setItems(
                query -> this.paymentService.getAllPayments(query.getPage(), query.getPageSize()).stream());
    }

    private void loadAccountsAsync() {
        LOG.info("Starting async account loading");

        this.loadDataAsync(
                () -> this.accountService.getAllAccounts(0, UiConstants.LARGE_PAGE_SIZE),
                accounts -> {
                    LOG.infof("Successfully loaded %s accounts", accounts.size());
                    this.availableAccounts = accounts;
                },
                throwable -> {
                    LOG.error("Error loading accounts", throwable);
                    this.availableAccounts = List.of();
                    MidasErrorHandler.handleInUiContext(throwable);
                },
                15);
    }

    private void buildUi() {
        this.removeAll();

        final var header = new H2("Payments");
        final var searchLayout = this.createSearchLayout();
        final var buttonLayout = this.createButtonLayout();
        this.createGrid();

        final Runnable authFailure = () -> this.getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        this.paymentDialog = new AdminPaymentDialog(
                this.paymentService, this.userService, this::setupLazyDataProvider, authFailure);
        this.availableAccounts = List.of();

        this.add(header, searchLayout, buttonLayout, this.grid);
    }

    private HorizontalLayout createSearchLayout() {
        final var searchLayout = new SearchLayout(
                e -> {
                        if (e.getValue() == null || e.getValue().trim().isEmpty()) {
                            this.setupLazyDataProvider();
                        }
                },
                _ -> this.searchPayments(),
                "Search by comment or other account...",
                "Search Payments");

        this.searchButton = searchLayout.getButton();
        this.searchField = searchLayout.getTextfield();

        // Recent payments filter
        final var recentFilterLayout = new IntegerFilterLayout(
                _ -> this.loadRecentPayments(),
                "How many?",
                "Load Recent",
                "100px");
        this.limitField = recentFilterLayout.getIntegerField();
        this.limitField.setMin(1);
        this.limitField.setMax(1000);
        this.limitField.setValue(10);

        // Date range filter
        final var dateFilterLayout = new DateFilterLayout(_ -> this.filterByDateRange());
        this.startDatePicker = dateFilterLayout.getStartDatePicker();
        this.endDatePicker = dateFilterLayout.getEndDatePicker();

        // User ID filter
        final var userFilterLayout = new IntegerFilterLayout(
                _ -> this.filterByUser(),
                "Enter User ID...",
                "Filter by User");
        this.userIdField = userFilterLayout.getIntegerField();

        searchLayout.add(recentFilterLayout, dateFilterLayout, userFilterLayout);
        return searchLayout;
    }

    private HorizontalLayout createButtonLayout() {
        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var refreshButton = new RefreshButton(_ -> this.setupLazyDataProvider());
        final var createButton = new CreateButton(
                _ -> this.paymentDialog.open(null, this.availableAccounts), "Create Payment");

        buttonLayout.add(refreshButton, createButton);
        return buttonLayout;
    }

    private void createGrid() {
        this.grid = new Grid<>(PaymentResponseDto.class, false);
        this.grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.grid.setSizeFull();

        // Configure columns using Response DTO fields with computed values
        this.grid.addColumn((ValueProvider<PaymentResponseDto, ?>) PaymentResponseDto::getId).setHeader("ID")
                .setWidth("80px").setFlexGrow(0);
        this.grid.addColumn((ValueProvider<PaymentResponseDto, ?>) PaymentResponseDto::getUsername).setHeader("User")
                .setWidth("100px").setFlexGrow(0);
        this.grid.addColumn((ValueProvider<PaymentResponseDto, ?>) PaymentResponseDto::getSourceAccountName)
                .setHeader("Sender").setWidth("150px")
                .setFlexGrow(0);
        this.grid.addColumn((ValueProvider<PaymentResponseDto, ?>) PaymentResponseDto::getTargetAccountName)
                .setHeader("Recipient").setWidth("150px")
                .setFlexGrow(0);
        this.grid.addColumn(payment -> payment.getAmount() != null ? payment.getAmount().toString() : "")
                .setHeader("Amount")
                .setWidth("120px").setFlexGrow(0);
        this.grid.addColumn((ValueProvider<PaymentResponseDto, ?>) PaymentResponseDto::getDate).setHeader("Date")
                .setWidth("120px").setFlexGrow(0);
        this.grid.addColumn((ValueProvider<PaymentResponseDto, ?>) PaymentResponseDto::getComment).setHeader("Comment")
                .setFlexGrow(1);
        this.grid.addColumn((ValueProvider<PaymentResponseDto, ?>) PaymentResponseDto::getCreated).setHeader("Created")
                .setWidth("120px").setFlexGrow(0);

        // Add action column
        this.grid.addComponentColumn(this::createActionButtons).setHeader("Actions").setWidth("150px").setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(final PaymentResponseDto payment) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var editButton = new EditButton(
                _ -> this.paymentDialog.open(payment.toPaymentDto(), this.availableAccounts));
        final var deleteButton = new DeleteButton(_ -> this.deletePayment(payment.toPaymentDto()));

        layout.add(editButton, deleteButton);
        return layout;
    }

    private void deletePayment(final PaymentDto payment) {
        try {
            if (this.paymentService.deletePayment(payment.getId())) {
                NotificationUtil.showSuccess("Payment deleted successfully");
                this.setupLazyDataProvider();
            } else {
                NotificationUtil.showError("Failed to delete payment");
            }
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        } catch (final ServiceException e) {
            NotificationUtil.showError(e, "Deleting payment failed. Please try again.");
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error deleting payment", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }

    private void loadRecentPayments() {
        final int limit = this.limitField.getValue() != null ? this.limitField.getValue() : 10;
        this.loadDataAsync(
                () -> this.paymentService.getRecentPayments(limit),
                payments -> this.grid.setItems(payments));
    }

    private void filterByDateRange() {
        final LocalDate startDate = this.startDatePicker.getValue();
        final LocalDate endDate = this.endDatePicker.getValue();

        if (startDate == null || endDate == null) {
            NotificationUtil.showWarning("Please select both start and end dates");
            return;
        }

        if (startDate.isAfter(endDate)) {
            NotificationUtil.showWarning("Start date must be before end date");
            return;
        }

        this.loadDataAsync(
                () -> this.paymentService.getPaymentsByDateRange(startDate, endDate),
                payments -> this.grid.setItems(payments));
    }

    private void filterByUser() {
        final Integer userId = this.userIdField.getValue();
        if (userId == null) {
            NotificationUtil.showWarning("Please enter a user ID");
            return;
        }

        this.loadDataAsync(
                () -> this.paymentService.getPaymentsByUser(userId.longValue()),
                payments -> this.grid.setItems(payments));
    }

    private void searchPayments() {
        final String query = this.searchField.getValue();
        if (query == null || query.trim().isEmpty()) {
            NotificationUtil.showWarning("Please enter a search query");
            return;
        }

        this.searchButton.setEnabled(false);
        this.searchButton.setText("Searching...");

        this.loadDataAsync(
                () -> this.paymentService.searchPayments(query.trim()),
                payments -> {
                    this.searchButton.setEnabled(true);
                    this.searchButton.setText("Search");
                    this.grid.setItems(payments);
                },
                throwable -> {
                    this.searchButton.setEnabled(true);
                    this.searchButton.setText("Search");
                    LOG.errorf(throwable, "Error searching payments: %s", throwable.getMessage());
                    MidasErrorHandler.handleInUiContext(throwable);
                });
    }
}
