package de.vptr.midas.gui.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import de.vptr.midas.gui.client.AccountClient;
import de.vptr.midas.gui.client.AccountClient.AssociateUserWithAccountRequest;
import de.vptr.midas.gui.client.AccountClient.CreateMyAccountPaymentRequest;
import de.vptr.midas.gui.client.AccountClient.DeleteAccountRequest;
import de.vptr.midas.gui.client.AccountClient.DeleteMyAccountPaymentRequest;
import de.vptr.midas.gui.client.AccountClient.GetAccountBalanceRequest;
import de.vptr.midas.gui.client.AccountClient.GetAccountByNameRequest;
import de.vptr.midas.gui.client.AccountClient.GetAccountRequest;
import de.vptr.midas.gui.client.AccountClient.GetAccountsByUserRequest;
import de.vptr.midas.gui.client.AccountClient.GetAllAccountsRequest;
import de.vptr.midas.gui.client.AccountClient.GetAssociatedUsersRequest;
import de.vptr.midas.gui.client.AccountClient.GetIncomingPaymentsRequest;
import de.vptr.midas.gui.client.AccountClient.GetMyAccountBalanceHistoryRequest;
import de.vptr.midas.gui.client.AccountClient.GetMyAccountBalanceRequest;
import de.vptr.midas.gui.client.AccountClient.GetMyAccountIncomingPaymentsRequest;
import de.vptr.midas.gui.client.AccountClient.GetMyAccountOutgoingPaymentsRequest;
import de.vptr.midas.gui.client.AccountClient.GetMyAccountPaymentsRequest;
import de.vptr.midas.gui.client.AccountClient.GetMyAccountsWithBalancesRequest;
import de.vptr.midas.gui.client.AccountClient.GetMyTotalBalanceHistoryRequest;
import de.vptr.midas.gui.client.AccountClient.GetOutgoingPaymentsRequest;
import de.vptr.midas.gui.client.AccountClient.RemoveUserFromAccountRequest;
import de.vptr.midas.gui.client.AccountClient.SearchAccountsRequest;
import de.vptr.midas.gui.client.AccountClient.UpdateAccountRequest;
import de.vptr.midas.gui.client.AccountClient.UpdateMyAccountPaymentRequest;
import de.vptr.midas.gui.client.AccountClient.UpdateUserAccountMetaRequest;
import de.vptr.midas.gui.dto.AccountDto;
import de.vptr.midas.gui.dto.AccountResponseDto;
import de.vptr.midas.gui.dto.BalancePointDto;
import de.vptr.midas.gui.dto.BalanceResponseDto;
import de.vptr.midas.gui.dto.PaymentDto;
import de.vptr.midas.gui.dto.PaymentResponseDto;
import de.vptr.midas.gui.dto.UserAccountMetaDto;
import de.vptr.midas.gui.dto.UserResponseDto;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.PaginationParamUtil;
import de.vptr.midas.gui.util.ServiceHelper;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

/** Service for account operations. */
@ApplicationScoped
public class AccountService {

    private static final Logger LOG = Logger.getLogger(AccountService.class);

    @Inject
    @RestClient
    AccountClient accountClient;

    @Inject
    AuthService authService;

    /** Returns the all accounts. */
    public List<AccountResponseDto> getAllAccounts(final Integer page, final Integer size) {
        LOG.debug("Fetching all accounts");
        final Integer safePage = PaginationParamUtil.sanitizePage(page);
        final Integer safeSize = PaginationParamUtil.sanitizeSize(size);
        return ServiceHelper.execute(
                () -> this.accountClient
                        .getAllAccounts(GetAllAccountsRequest.newInstance().page(safePage).size(safeSize)),
                this.authService, LOG, "fetching accounts");
    }

    /** Returns the account by id, or empty if it does not exist. */
    public Optional<AccountDto> getAccountById(final String id) {
        LOG.debugf("Fetching account with ID: %s", id);
        return ServiceHelper
                .executeOptional(() -> this.accountClient.getAccount(GetAccountRequest.newInstance().publicId(id)),
                        this.authService, LOG, "fetching account " + id)
                .map(AccountDto::from);
    }

    /** Returns the account by name, or empty if it does not exist. */
    public Optional<AccountDto> getAccountByName(final String name) {
        LOG.debugf("Fetching account with name: %s", name);
        return ServiceHelper.executeOptional(
                () -> this.accountClient.getAccountByName(GetAccountByNameRequest.newInstance().name(name)),
                this.authService, LOG, "fetching account by name " + name).map(AccountDto::from);
    }

    /** Returns the accounts by user. */
    public List<AccountResponseDto> getAccountsByUser(final String userId) {
        LOG.debugf("Fetching accounts for user: %s", userId);
        return ServiceHelper.execute(
                () -> this.accountClient.getAccountsByUser(GetAccountsByUserRequest.newInstance().userPublicId(userId)),
                this.authService, LOG, "fetching accounts for user " + userId);
    }

    // User-facing method that doesn't require admin permissions
    /** Returns the my accounts. */
    public List<AccountResponseDto> getMyAccounts() {
        LOG.debug("Fetching accounts for current user");
        return ServiceHelper.execute(() -> this.accountClient.getMyAccounts(), this.authService, LOG,
                "fetching my accounts");
    }

    // Optimized method to get accounts with balances in a single call
    /**
     * Returns the current user's accounts with server-computed balances. When both {@code startDate} and
     * {@code endDate} are provided each balance is restricted to that inclusive date range.
     */
    public List<AccountResponseDto> getMyAccountsWithBalances(@Nullable final LocalDate startDate,
            @Nullable final LocalDate endDate) {
        LOG.debug("Fetching accounts with balances for current user");
        return ServiceHelper.execute(
                () -> this.accountClient.getMyAccountsWithBalances(
                        GetMyAccountsWithBalancesRequest.newInstance().startDate(startDate).endDate(endDate)
                                .page(PaginationParamUtil.DEFAULT_PAGE).size(PaginationParamUtil.MAX_SIZE)),
                this.authService, LOG, "fetching my accounts with balances");
    }

    /** Handles the search accounts operation. */
    public List<AccountResponseDto> searchAccounts(final String query) {
        LOG.debugf("Searching accounts with query: %s", query);
        return ServiceHelper.execute(
                () -> this.accountClient.searchAccounts(SearchAccountsRequest.newInstance().query(query)),
                this.authService, LOG, "searching accounts");
    }

    /** Returns the outgoing payments. */
    public List<PaymentDto> getOutgoingPayments(final String accountId) {
        LOG.debugf("Fetching outgoing payments for account: %s", accountId);
        return ServiceHelper
                .execute(
                        () -> this.accountClient
                                .getOutgoingPayments(GetOutgoingPaymentsRequest.newInstance().publicId(accountId)),
                        this.authService, LOG, "fetching outgoing payments for account " + accountId)
                .stream().map(PaymentDto::from).toList();
    }

    /** Returns the incoming payments. */
    public List<PaymentDto> getIncomingPayments(final String accountId) {
        LOG.debugf("Fetching incoming payments for account: %s", accountId);
        return ServiceHelper
                .execute(
                        () -> this.accountClient
                                .getIncomingPayments(GetIncomingPaymentsRequest.newInstance().publicId(accountId)),
                        this.authService, LOG, "fetching incoming payments for account " + accountId)
                .stream().map(PaymentDto::from).toList();
    }

    /** Returns the associated users. */
    public List<UserResponseDto> getAssociatedUsers(final String accountId) {
        LOG.debugf("Fetching users associated with account: %s", accountId);
        return ServiceHelper.execute(
                () -> this.accountClient
                        .getAssociatedUsers(GetAssociatedUsersRequest.newInstance().publicId(accountId)),
                this.authService, LOG, "fetching users for account " + accountId);
    }

    /** Creates a new account. */
    public AccountDto createAccount(final AccountDto account) {
        LOG.debugf("Creating new account: %s", account);
        return AccountDto.from(ServiceHelper.execute(() -> this.accountClient.createAccount(account), this.authService,
                LOG, "creating account", "Failed to create account"));
    }

    /** Updates the account. */
    public AccountDto updateAccount(final AccountDto account) {
        final String publicId = Objects.requireNonNull(account.getPublicId(), "account.publicId");
        LOG.debugf("Updating account: %s", account);
        return AccountDto.from(ServiceHelper.execute(
                () -> this.accountClient.updateAccount(UpdateAccountRequest.newInstance().publicId(publicId), account),
                this.authService, LOG, "updating account " + publicId, "Failed to update account"));
    }

    /** Handles the delete account operation. */
    public void deleteAccount(final String id) {
        LOG.debugf("Deleting account with ID: %s", id);
        ServiceHelper.executeVoid(
                () -> this.accountClient.deleteAccount(DeleteAccountRequest.newInstance().publicId(id)),
                this.authService, LOG, "deleting account " + id);
    }

    /** Handles the associate user with account operation. */
    public void associateUserWithAccount(final String accountId, final String userId) {
        LOG.debugf("Associating user %s with account %s", userId, accountId);
        ServiceHelper.executeVoid(() -> {
            try {
                this.accountClient.associateUserWithAccount(
                        AssociateUserWithAccountRequest.newInstance().accountPublicId(accountId).userPublicId(userId));
            } catch (final WebApplicationException e) {
                if (e.getResponse().getStatus() == 409) {
                    e.getResponse().close();
                    throw new ServiceException("User is already associated with this account");
                }
                throw e;
            }
        }, this.authService, LOG, "associating user " + userId + " with account " + accountId);
    }

    /** Removes the user from account. */
    public void removeUserFromAccount(final String accountId, final String userId) {
        LOG.debugf("Removing user %s from account %s", userId, accountId);
        ServiceHelper.executeVoid(
                () -> this.accountClient.removeUserFromAccount(
                        RemoveUserFromAccountRequest.newInstance().accountPublicId(accountId).userPublicId(userId)),
                this.authService, LOG, "removing user " + userId + " from account " + accountId);
    }

    /** Updates the user-account meta (e.g. isNegative, canManage flags) for the given association. */
    public void updateUserAccountMeta(final String accountId, final String userId, final boolean isNegative,
            final boolean canManage) {
        LOG.debugf("Updating user-account meta: accountId=%s, userId=%s, isNegative=%s, canManage=%s", accountId,
                userId, isNegative, canManage);
        final var dto = new UserAccountMetaDto();
        dto.setIsNegative(isNegative);
        dto.setCanManage(canManage);
        ServiceHelper
                .executeVoid(
                        () -> this.accountClient.updateUserAccountMeta(UpdateUserAccountMetaRequest.newInstance()
                                .accountPublicId(accountId).userPublicId(userId), dto),
                        this.authService, LOG, "updating user-account meta");
    }

    /** Deletes the my account payment. */
    public void deleteMyAccountPayment(final String accountId, final String paymentId) {
        LOG.debugf("Deleting payment %s for my account: %s", paymentId, accountId);
        ServiceHelper.executeVoid(
                () -> this.accountClient.deleteMyAccountPayment(DeleteMyAccountPaymentRequest.newInstance()
                        .accountPublicId(accountId).paymentPublicId(paymentId)),
                this.authService, LOG, "deleting payment " + paymentId + " for my account " + accountId);
    }

    /** Returns the account balance. */
    public BigDecimal getAccountBalance(final String accountId) {
        LOG.debugf("Fetching balance for account: %s", accountId);
        return requireBalance(ServiceHelper.execute(
                () -> this.accountClient.getAccountBalance(GetAccountBalanceRequest.newInstance().publicId(accountId)),
                this.authService, LOG, "fetching balance for account " + accountId));
    }

    // User-facing method that checks account access before returning balance
    /**
     * Returns the balance for one of the current user's accounts. When both {@code startDate} and {@code endDate} are
     * provided the balance is restricted to that inclusive date range, computed server-side.
     */
    public BigDecimal getMyAccountBalance(final String accountId, @Nullable final LocalDate startDate,
            @Nullable final LocalDate endDate) {
        LOG.debugf("Fetching balance for my account: %s", accountId);
        return requireBalance(ServiceHelper.execute(
                () -> this.accountClient.getMyAccountBalance(GetMyAccountBalanceRequest.newInstance()
                        .publicId(accountId).startDate(startDate).endDate(endDate)),
                this.authService, LOG, "fetching balance for my account " + accountId));
    }

    /**
     * Returns the cumulative balance-over-time series for one of the current user's accounts. When both
     * {@code startDate} and {@code endDate} are provided the series is restricted to that inclusive date range,
     * computed server-side.
     */
    public List<BalancePointDto> getMyAccountBalanceHistory(final String accountId, @Nullable final LocalDate startDate,
            @Nullable final LocalDate endDate) {
        LOG.debugf("Fetching balance history for my account: %s", accountId);
        return ServiceHelper.execute(
                () -> this.accountClient.getMyAccountBalanceHistory(GetMyAccountBalanceHistoryRequest.newInstance()
                        .publicId(accountId).startDate(startDate).endDate(endDate)),
                this.authService, LOG, "fetching balance history for my account " + accountId);
    }

    /**
     * Returns the cumulative total balance-over-time series across all of the current user's accounts. When both
     * {@code startDate} and {@code endDate} are provided the series is restricted to that inclusive date range,
     * computed server-side.
     */
    public List<BalancePointDto> getMyTotalBalanceHistory(@Nullable final LocalDate startDate,
            @Nullable final LocalDate endDate) {
        LOG.debug("Fetching total balance history for current user");
        return ServiceHelper.execute(
                () -> this.accountClient.getMyTotalBalanceHistory(
                        GetMyTotalBalanceHistoryRequest.newInstance().startDate(startDate).endDate(endDate)),
                this.authService, LOG, "fetching total balance history");
    }

    private static BigDecimal requireBalance(@Nullable final BalanceResponseDto balanceResponse) {
        final BigDecimal balance = balanceResponse == null ? null : balanceResponse.getBalance();
        if (balance == null) {
            throw new ServiceException("Malformed balance payload: missing balance");
        }
        return balance;
    }

    // User-facing method that checks account access before returning payments
    /**
     * Returns a single page of payments for one of the current user's accounts. Date-range and free-text filtering as
     * well as pagination are applied server-side so the full payment set is never loaded into GUI memory.
     */
    public List<PaymentResponseDto> getMyAccountPayments(final String accountId, @Nullable final LocalDate startDate,
            @Nullable final LocalDate endDate, @Nullable final String query, final Integer page, final Integer size) {
        LOG.debugf("Fetching payments for my account: %s", accountId);
        final Integer safePage = PaginationParamUtil.sanitizePage(page);
        final Integer safeSize = PaginationParamUtil.sanitizeSize(size);
        return ServiceHelper.execute(
                () -> this.accountClient
                        .getMyAccountPayments(GetMyAccountPaymentsRequest.newInstance().publicId(accountId)
                                .startDate(startDate).endDate(endDate).page(safePage).query(query).size(safeSize)),
                this.authService, LOG, "fetching payments for my account " + accountId);
    }

    /** Returns payments grouped by account publicId for all of the user's accounts. */
    public Map<String, List<PaymentResponseDto>> getMyAccountsPayments() {
        LOG.debug("Fetching payments grouped by account for current user");
        return ServiceHelper.execute(() -> this.accountClient.getMyAccountsPayments(), this.authService, LOG,
                "fetching my accounts payments");
    }

    // User-facing method that checks account access before returning outgoing
    // payments
    /** Returns the my account outgoing payments. */
    public List<PaymentResponseDto> getMyAccountOutgoingPayments(final String accountId) {
        LOG.debugf("Fetching outgoing payments for my account: %s", accountId);
        return ServiceHelper.execute(
                () -> this.accountClient.getMyAccountOutgoingPayments(
                        GetMyAccountOutgoingPaymentsRequest.newInstance().publicId(accountId)),
                this.authService, LOG, "fetching outgoing payments for my account " + accountId);
    }

    // User-facing method that checks account access before returning incoming
    // payments
    /** Returns the my account incoming payments. */
    public List<PaymentResponseDto> getMyAccountIncomingPayments(final String accountId) {
        LOG.debugf("Fetching incoming payments for my account: %s", accountId);
        return ServiceHelper.execute(
                () -> this.accountClient.getMyAccountIncomingPayments(
                        GetMyAccountIncomingPaymentsRequest.newInstance().publicId(accountId)),
                this.authService, LOG, "fetching incoming payments for my account " + accountId);
    }

    // User-facing method that checks account access before creating payment
    /** Creates a new my account payment. */
    public PaymentDto createMyAccountPayment(final String accountId, final PaymentDto payment) {
        LOG.debugf("Creating payment for my account: %s", accountId);
        return PaymentDto.from(ServiceHelper.execute(
                () -> this.accountClient.createMyAccountPayment(
                        CreateMyAccountPaymentRequest.newInstance().accountPublicId(accountId), payment),
                this.authService, LOG, "creating payment for my account " + accountId, "Failed to create payment"));
    }

    // User-facing method that checks account access before updating payment
    /** Updates the my account payment. */
    public PaymentDto updateMyAccountPayment(final String accountId, final String paymentId, final PaymentDto payment) {
        LOG.debugf("Updating payment %s for my account: %s", paymentId, accountId);
        return PaymentDto.from(ServiceHelper.execute(
                () -> this.accountClient.updateMyAccountPayment(UpdateMyAccountPaymentRequest.newInstance()
                        .accountPublicId(accountId).paymentPublicId(paymentId), payment),
                this.authService, LOG, "updating payment " + paymentId + " for my account " + accountId,
                "Failed to update payment"));
    }
}
