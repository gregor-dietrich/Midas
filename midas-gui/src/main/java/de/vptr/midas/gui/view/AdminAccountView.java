package de.vptr.midas.gui.view;

import java.util.Objects;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.function.ValueProvider;
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
import de.vptr.midas.gui.service.AccountService;
import de.vptr.midas.gui.service.UserService;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/** Admin view for managing accounts. */
@Route(value = "admin/accounts", layout = AdminMainLayout.class)
public class AdminAccountView extends AbstractAdminEntityView<AccountResponseDto> {

    @Inject
    transient AccountService accountService;

    @Inject
    transient UserService userService;

    @Nullable
    private AdminAccountDialog accountDialog;
    @Nullable
    private AdminAccountUserPanel userPanel;

    @Override
    protected void onBeforeEnterAuthenticated() {
        this.setupLazyDataProvider();
    }

    @Override
    protected String getViewTitle() {
        return "Accounts";
    }

    @Override
    protected Class<AccountResponseDto> getEntityClass() {
        return AccountResponseDto.class;
    }

    @Override
    protected String getEntityNameSingle() {
        return "Account";
    }

    @Override
    protected String getSearchPlaceholder() {
        return "Search by name...";
    }

    @Override
    protected String getSearchButtonText() {
        return "Search Accounts";
    }

    @Override
    protected void addCustomFilterLayouts(final SearchLayout searchLayout) {
        final var filterLayout =
                new IntegerFilterLayout(ignore -> this.filterByUser(), "Enter ID...", "Filter by User");

        this.userIdField = filterLayout.getIntegerField();
        searchLayout.add(filterLayout);
    }

    @Override
    protected void executeUserFilter(final Integer userId) {
        this.loadDataAsync(() -> this.accountService.getAccountsByUser(userId.longValue()),
                accounts -> Objects.requireNonNull(this.grid).setItems(accounts));
    }

    @Override
    protected void initAdditionalComponents() {
        final Runnable onAuthFailure = this.getAuthFailureRunnable();
        this.accountDialog = new AdminAccountDialog(this.accountService, this::setupLazyDataProvider, onAuthFailure);
        this.userPanel =
                new AdminAccountUserPanel(this.accountService, this.authService, this.userService, onAuthFailure);
    }

    @Override
    protected void setupLazyDataProvider() {
        Objects.requireNonNull(this.grid)
                .setItems(query -> this.accountService.getAllAccounts(query.getPage(), query.getPageSize()).stream());
    }

    @Override
    protected void executeSearch() {
        final String query = Objects.requireNonNull(this.searchField).getValue();
        this.executeAsyncSearch(query, q -> this.accountService.searchAccounts(q.trim()));
    }

    @Override
    protected HorizontalLayout createButtonLayout() {
        final var buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);

        final var refreshButton = new RefreshButton(ignore -> this.setupLazyDataProvider());
        final var createButton =
                new CreateButton(ignore -> Objects.requireNonNull(this.accountDialog).open(null), "Create Account");

        buttonLayout.add(refreshButton, createButton);
        return buttonLayout;
    }

    @Override
    protected void configureGrid(final Grid<AccountResponseDto> g) {
        g.addColumn((ValueProvider<AccountResponseDto, ?>) AccountResponseDto::getId).setHeader("ID").setWidth("80px")
                .setFlexGrow(0);

        g.addComponentColumn(account -> this.createClickableSpan(account.getName(),
                ignore -> Objects.requireNonNull(this.accountDialog).open(account))).setHeader("Name").setFlexGrow(1);

        g.addComponentColumn(this::createActionButtons).setHeader("Actions").setWidth("180px").setFlexGrow(0);
    }

    private HorizontalLayout createActionButtons(final AccountResponseDto account) {
        final var layout = new HorizontalLayout();
        layout.setSpacing(true);

        final var editButton = new EditButton(ignore -> Objects.requireNonNull(this.accountDialog).open(account));
        final var deleteButton = new DeleteButton(ignore -> this.handleDeletion(() -> {
            this.accountService.deleteAccount(Objects.requireNonNull(account.getId()));
        }));
        final var manageUsersButton =
                new ManageUsersButton(ignore -> Objects.requireNonNull(this.userPanel).open(account));

        layout.add(editButton, deleteButton, manageUsersButton);
        return layout;
    }
}
