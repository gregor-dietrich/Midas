package de.vptr.midas.gui.view;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
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
import de.vptr.midas.gui.component.button.ManageUsersButton;
import de.vptr.midas.gui.component.button.RefreshButton;
import de.vptr.midas.gui.component.dialog.AdminAccountDialog;
import de.vptr.midas.gui.component.dialog.AdminAccountUserPanel;
import de.vptr.midas.gui.component.layout.IntegerFilterLayout;
import de.vptr.midas.gui.component.layout.SearchLayout;
import de.vptr.midas.gui.dto.AccountResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.service.AccountService;
import de.vptr.midas.gui.service.AuthService;
import de.vptr.midas.gui.service.UserService;
import de.vptr.midas.gui.util.ErrorHandler;
import de.vptr.midas.gui.util.NotificationUtil;
import de.vptr.midas.gui.util.UiConstants;
import jakarta.inject.Inject;

/** Admin view for managing accounts. */
@Route(value = "admin/accounts", layout = AdminMainLayout.class)
public class AdminAccountView extends AbstractAsyncDataView implements BeforeEnterObserver {

    private static final Logger LOG = Logger.getLogger(AdminAccountView.class);

    @Inject
    AccountService accountService;

    @Inject
    AuthService authService;

    @Inject
    UserService userService;

    private Grid<AccountResponseDto> grid;
    private TextField searchField;
    private Button searchButton;
    private IntegerField userIdField;

    private AdminAccountDialog accountDialog;
    private AdminAccountUserPanel userPanel;

    /** Initializes AdminAccountView. */
    public AdminAccountView() {
        this.setSizeFull();
        this.setSpacing(true);
        this.setPadding(true);
    }

    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        LOG.info("AdminAccountView.beforeEnter - Starting view initialization");
        this.buildUi();
        this.loadAccountsAsync();
        LOG.info("AdminAccountView.beforeEnter - View initialization completed");
    }

    private void loadAccountsAsync() {
        final String authHeader;
        try {
            authHeader = this.authService.getAuthorizationHeader();
        } catch (final RuntimeException e) {
            NotificationUtil.showError("Authentication failed");
            return;
        }
        this.loadDataAsync(
                () -> this.accountService.getAllAccounts(authHeader, 0, UiConstants.DEFAULT_PAGE_SIZE),
                accounts -> this.grid.setItems(accounts));
    }

    private void buildUi() {
        this.removeAll();

        final Runnable onAuthFailure = () -> this.getUI().ifPresent(ui -> ui.navigate(LoginView.class));

        final var header = new H2("Accounts");
        final var searchLayout = this.createSearchLayout();
        final var buttonLayout = this.createButtonLayout();
        this.createGrid();
        this.accountDialog = new AdminAccountDialog(this.accountService, this::loadAccountsAsync, onAuthFailure);
        this.userPanel = new AdminAccountUserPanel(this.accountService, this.authService, this.userService,
                onAuthFailure);

        this.add(header, searchLayout, buttonLayout, this.grid);
    }

    private HorizontalLayout createSearchLayout() {
        final var searchLayout = new SearchLayout(
                e -> {
                    if (e.getValue() == null || e.getValue().trim().isEmpty()) {
                        this.loadAccountsAsync();
                    }
                },
                _ -> this.searchAccounts(),
                "Search by name...",
                "Search Accounts");

        this.searchButton = searchLayout.getButton();
        this.searchField = searchLayout.getTextfield();

        final var filterLayout = new IntegerFilterLayout(
                _ -> this.filterByUser(),
                "Enter ID...",
                "Filter by User");

        this.userIdField = filterLayout.getIntegerField();

        searchLayout.add(filterLayout);
        return searchLayout;
    }

    private HorizontalLayout createButtonLayout() {
        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var refreshButton = new RefreshButton(_ -> this.loadAccountsAsync());
        final var createButton = new CreateButton(_ -> this.accountDialog.open(null), "Create Account");

        buttonLayout.add(refreshButton, createButton);
        return buttonLayout;
    }

    private void createGrid() {
        this.grid = new Grid<>(AccountResponseDto.class, false);
        this.grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        this.grid.setSizeFull();

        // Configure columns
        this.grid.addColumn((ValueProvider<AccountResponseDto, ?>) AccountResponseDto::getId).setHeader("ID")
                .setWidth("80px").setFlexGrow(0);

        // Make the name column clickable
        this.grid.addComponentColumn(account -> {
            final var nameSpan = new Span(account.getName());
            nameSpan.getStyle().set("color", "var(--lumo-primary-text-color)");
            nameSpan.getStyle().set("cursor", "pointer");
            nameSpan.getStyle().set("width", "100%");
            nameSpan.getStyle().set("display", "block");
            nameSpan.addClickListener(_ -> this.accountDialog.open(account));
            return nameSpan;
        }).setHeader("Name").setFlexGrow(1);

        // Add action column
        this.grid.addComponentColumn(this::createActionButtons).setHeader("Actions").setWidth("180px").setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(final AccountResponseDto account) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var editButton = new EditButton(_ -> this.accountDialog.open(account));
        final var deleteButton = new DeleteButton(_ -> this.deleteAccount(account));
        final var manageUsersButton = new ManageUsersButton(_ -> this.userPanel.open(account));

        layout.add(editButton, deleteButton, manageUsersButton);
        return layout;
    }

    private void searchAccounts() {
        final String query = this.searchField.getValue();
        if (query == null || query.trim().isEmpty()) {
            NotificationUtil.showWarning("Please enter a search query");
            return;
        }
        final String authHeader;
        try {
            authHeader = this.authService.getAuthorizationHeader();
        } catch (final RuntimeException e) {
            NotificationUtil.showError("Authentication failed");
            return;
        }
        this.searchButton.setEnabled(false);
        this.searchButton.setText("Searching...");
        this.loadDataAsync(
                () -> this.accountService.searchAccounts(query.trim(), authHeader),
                accounts -> {
                    this.searchButton.setEnabled(true);
                    this.searchButton.setText("Search Accounts");
                    this.grid.setItems(accounts);
                },
                throwable -> {
                    this.searchButton.setEnabled(true);
                    this.searchButton.setText("Search Accounts");
                    ErrorHandler.handleInUiContext(throwable);
                });
    }

    private void filterByUser() {
        final Integer value = this.userIdField.getValue();
        if (value == null) {
            NotificationUtil.showWarning("Please enter a user ID");
            return;
        }

        this.loadDataAsync(
                () -> this.accountService.getAccountsByUser((long) value),
                accounts -> this.grid.setItems(accounts));
    }

    private void deleteAccount(final AccountResponseDto account) {
        try {
            final var accountDto = account.toAccountDto();
            if (this.accountService.deleteAccount(accountDto.getId())) {
                NotificationUtil.showSuccess("Account deleted successfully");
                this.loadAccountsAsync();
            } else {
                NotificationUtil.showError("Failed to delete account");
            }
        } catch (final AuthenticationException e) {
            NotificationUtil.showError("Session expired. Please log in again.");
            this.getUI().ifPresent(ui -> ui.navigate(LoginView.class));
        } catch (final ServiceException e) {
            NotificationUtil.showError(e, "Deleting account failed. Please try again.");
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error deleting account", e);
            NotificationUtil.showError("Unexpected error occurred");
        }
    }
}
