package de.vptr.midas.gui.service;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.vptr.midas.gui.client.AccountClient;
import de.vptr.midas.gui.dto.AccountDto;
import de.vptr.midas.gui.dto.AccountResponseDto;
import de.vptr.midas.gui.dto.BalanceResponseDto;
import de.vptr.midas.gui.dto.PaymentDto;
import de.vptr.midas.gui.dto.PaymentResponseDto;
import de.vptr.midas.gui.dto.UserAccountMetaDto;
import de.vptr.midas.gui.dto.UserResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
import de.vptr.midas.gui.util.PaginationParamUtil;
import de.vptr.midas.gui.util.ServiceHelper;
import de.vptr.midas.gui.util.UiConstants;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;

/** Service for account operations. */
@ApplicationScoped
public class AccountService {

    private static final Logger LOG = Logger.getLogger(AccountService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Inject
    @RestClient
    AccountClient accountClient;

    @Inject
    AuthService authService;

    /** Returns the all accounts. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public List<AccountResponseDto> getAllAccounts(final Integer page, final Integer size) {
        LOG.debug("Fetching all accounts");
        final var authHeader = this.authService.requireAuthorizationHeader();
        final Integer safePage = PaginationParamUtil.sanitizePage(page);
        final Integer safeSize = PaginationParamUtil.sanitizeSize(size);
        return ServiceHelper.execute(() -> this.accountClient.getAllAccounts(safePage, safeSize, authHeader),
                this.authService, LOG, "fetching accounts");
    }

    /** Returns the account by id. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public Optional<AccountDto> getAccountById(final Long id) {
        LOG.debugf("Fetching account with ID: %s", id);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.accountClient.getAccount(id, authHeader), this.authService,
                LOG, "fetching account " + id, AccountService::mapAccountOptionalResponse);
    }

    /** Returns the account by name. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public Optional<AccountDto> getAccountByName(final String name) {
        LOG.debugf("Fetching account with name: %s", name);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.accountClient.getAccountByName(name, authHeader),
                this.authService, LOG, "fetching account by name " + name, AccountService::mapAccountOptionalResponse);
    }

    /** Returns the accounts by user. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public List<AccountResponseDto> getAccountsByUser(final Long userId) {
        LOG.debugf("Fetching accounts for user: %s", userId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.accountClient.getAccountsByUser(userId, authHeader), this.authService,
                LOG, "fetching accounts for user " + userId);
    }

    // User-facing method that doesn't require admin permissions
    /** Returns the my accounts. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public List<AccountResponseDto> getMyAccounts() {
        LOG.debug("Fetching accounts for current user");
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.accountClient.getMyAccounts(authHeader), this.authService, LOG,
                "fetching my accounts");
    }

    // Optimized method to get accounts with balances in a single call
    /** Returns the my accounts with balances. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public List<AccountResponseDto> getMyAccountsWithBalances() {
        LOG.debug("Fetching accounts with balances for current user");
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.accountClient.getMyAccountsWithBalances(authHeader), this.authService,
                LOG, "fetching my accounts with balances");
    }

    /** Handles the search accounts operation. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public List<AccountResponseDto> searchAccounts(final String query) {
        LOG.debugf("Searching accounts with query: %s", query);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.accountClient.searchAccounts(query, authHeader), this.authService, LOG,
                "searching accounts");
    }

    /** Returns the outgoing payments. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public List<PaymentDto> getOutgoingPayments(final Long accountId) {
        LOG.debugf("Fetching outgoing payments for account: %s", accountId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.accountClient.getOutgoingPayments(accountId, authHeader),
                this.authService, LOG, "fetching outgoing payments for account " + accountId,
                response -> mapPaymentsListResponse(response, "outgoing payments response"));
    }

    /** Returns the incoming payments. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public List<PaymentDto> getIncomingPayments(final Long accountId) {
        LOG.debugf("Fetching incoming payments for account: %s", accountId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.accountClient.getIncomingPayments(accountId, authHeader),
                this.authService, LOG, "fetching incoming payments for account " + accountId,
                response -> mapPaymentsListResponse(response, "incoming payments response"));
    }

    /** Returns the associated users. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public List<UserResponseDto> getAssociatedUsers(final Long accountId) {
        LOG.debugf("Fetching users associated with account: %s", accountId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.accountClient.getAssociatedUsers(accountId, authHeader),
                this.authService, LOG, "fetching users for account " + accountId);
    }

    /** Creates a new account. */
    public AccountDto createAccount(final AccountDto account) {
        LOG.debugf("Creating new account: %s", account);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.accountClient.createAccount(account, authHeader),
                this.authService, LOG, "creating account", "Failed to create account",
                response -> ServiceHelper.readExpectedStatusEntity(response, 201, AccountResponseDto.class,
                        AccountResponseDto::toAccountDto, "created account response", "Failed to create account"));
    }

    /** Updates the account. */
    public AccountDto updateAccount(final AccountDto account) {
        LOG.debugf("Updating account: %s", account);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper
                .executeWithResponse(() -> this.accountClient.updateAccount(account.getId(), account, authHeader),
                        this.authService, LOG, "updating account " + account.getId(), "Failed to update account",
                        response -> ServiceHelper.readExpectedStatusEntity(response, 200, AccountResponseDto.class,
                                AccountResponseDto::toAccountDto, "updated account response",
                                "Failed to update account"));
    }

    /** Handles the delete account operation. */
    public boolean deleteAccount(final Long id) {
        LOG.debugf("Deleting account with ID: %s", id);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.accountClient.deleteAccount(id, authHeader),
                this.authService, LOG, "deleting account " + id, response -> response.getStatus() == 204);
    }

    /** Handles the associate user with account operation. */
    public boolean associateUserWithAccount(final Long accountId, final Long userId) {
        LOG.debugf("Associating user %s with account %s", userId, accountId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> {
            try {
                return this.accountClient.associateUserWithAccount(accountId, userId, authHeader);
            } catch (final WebApplicationException e) {
                if (e.getResponse().getStatus() == 409) {
                    throw new ServiceException("User is already associated with this account");
                }
                throw e;
            }
        }, this.authService, LOG, "associating user " + userId + " with account " + accountId,
                response -> response.getStatus() == 201);
    }

    /** Removes the user from account. */
    public boolean removeUserFromAccount(final Long accountId, final Long userId) {
        LOG.debugf("Removing user %s from account %s", userId, accountId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(
                () -> this.accountClient.removeUserFromAccount(accountId, userId, authHeader), this.authService, LOG,
                "removing user " + userId + " from account " + accountId, response -> response.getStatus() == 204);
    }

    /** Updates the user-account meta (e.g. isNegative flag) for the given association. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public void updateUserAccountMeta(final Long accountId, final Long userId, final boolean isNegative) {
        LOG.debugf("Updating user-account meta: accountId=%s, userId=%s, isNegative=%s", accountId, userId,
                (Object) isNegative);
        final var authHeader = this.authService.requireAuthorizationHeader();
        final var dto = new UserAccountMetaDto();
        dto.setIsNegative(isNegative);
        ServiceHelper.executeWithResponse(
                () -> this.accountClient.updateUserAccountMeta(accountId, userId, dto, authHeader), this.authService,
                LOG, "updating user-account meta", response -> {
                    if (response.getStatus() != 204) {
                        throw ServiceHelper.responseFailure("Failed to update user-account meta", response);
                    }
                    return null;
                });
    }

    /** Returns the account balance. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public BigDecimal getAccountBalance(final Long accountId) {
        LOG.debugf("Fetching balance for account: %s", accountId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.accountClient.getAccountBalance(accountId, authHeader),
                this.authService, LOG, "fetching balance for account " + accountId, response -> {
                    if (response.getStatus() == 200) {
                        return parseBalancePayload(response);
                    } else {
                        throw ServiceHelper.backendError(response);
                    }
                });
    }

    // User-facing method that checks account access before returning balance
    /** Returns the my account balance. */
    public BigDecimal getMyAccountBalance(final Long accountId) {
        LOG.debugf("Fetching balance for my account: %s", accountId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.accountClient.getMyAccountBalance(accountId, authHeader),
                this.authService, LOG, "fetching balance for my account " + accountId, response -> {
                    if (response.getStatus() == 200) {
                        return parseBalancePayload(response);
                    } else if (response.getStatus() == 403) {
                        throw new ServiceException("Access denied to this account");
                    } else {
                        throw ServiceHelper.backendError(response);
                    }
                });
    }

    private static BigDecimal parseBalancePayload(final Response response) {
        final String jsonResponse = ServiceHelper.readEntity(response, String.class, "balance payload");
        try {
            final BalanceResponseDto balanceResponse = OBJECT_MAPPER.readValue(jsonResponse, BalanceResponseDto.class);
            if (balanceResponse == null || balanceResponse.getBalance() == null) {
                throw new ServiceException("Malformed balance payload: missing balance");
            }
            return balanceResponse.getBalance();
        } catch (final JsonProcessingException | IllegalArgumentException e) {
            throw new ServiceException("Malformed balance payload", e);
        }
    }

    // User-facing method that checks account access before returning payments
    /** Returns the my account payments. */
    public List<PaymentResponseDto> getMyAccountPayments(final Long accountId) {
        LOG.debugf("Fetching payments for my account: %s", accountId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.accountClient.getMyAccountPayments(accountId, authHeader),
                this.authService, LOG, "fetching payments for my account " + accountId);
    }

    // User-facing method that checks account access before returning outgoing
    // payments
    /** Returns the my account outgoing payments. */
    public List<PaymentResponseDto> getMyAccountOutgoingPayments(final Long accountId) {
        LOG.debugf("Fetching outgoing payments for my account: %s", accountId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.accountClient.getMyAccountOutgoingPayments(accountId, authHeader),
                this.authService, LOG, "fetching outgoing payments for my account " + accountId);
    }

    // User-facing method that checks account access before returning incoming
    // payments
    /** Returns the my account incoming payments. */
    public List<PaymentResponseDto> getMyAccountIncomingPayments(final Long accountId) {
        LOG.debugf("Fetching incoming payments for my account: %s", accountId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.accountClient.getMyAccountIncomingPayments(accountId, authHeader),
                this.authService, LOG, "fetching incoming payments for my account " + accountId);
    }

    // User-facing method that checks account access before creating payment
    /** Creates a new my account payment. */
    public PaymentDto createMyAccountPayment(final Long accountId, final PaymentDto payment) {
        LOG.debugf("Creating payment for my account: %s", accountId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(
                () -> this.accountClient.createMyAccountPayment(accountId, payment, authHeader), this.authService, LOG,
                "creating payment for my account " + accountId, "Failed to create payment", response -> {
                    if (response.getStatus() == 201) {
                        final PaymentResponseDto responseDto = ServiceHelper.readEntity(response,
                                PaymentResponseDto.class, "created payment response");
                        return responseDto.toPaymentDto();
                    }
                    throw ServiceHelper.responseFailure("Failed to create payment", response);
                });
    }

    // User-facing method that checks account access before updating payment
    /** Updates the my account payment. */
    public PaymentDto updateMyAccountPayment(final Long accountId, final Long paymentId, final PaymentDto payment) {
        LOG.debugf("Updating payment %s for my account: %s", paymentId, accountId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(
                () -> this.accountClient.updateMyAccountPayment(accountId, paymentId, payment, authHeader),
                this.authService, LOG, "updating payment " + paymentId + " for my account " + accountId,
                "Failed to update payment",
                response -> ServiceHelper.readExpectedStatusEntity(response, 200, PaymentResponseDto.class,
                        PaymentResponseDto::toPaymentDto, "updated payment response", "Failed to update payment"));
    }

    private static Optional<AccountDto> mapAccountOptionalResponse(final Response response) {
        return ServiceHelper.readOptionalEntity(response, AccountResponseDto.class, AccountResponseDto::toAccountDto,
                "account response");
    }

    private static List<PaymentDto> mapPaymentsListResponse(final Response response, final String entityName) {
        if (response.getStatus() == 200) {
            return ServiceHelper.readEntity(response, new GenericType<List<PaymentDto>>() {
            }, entityName);
        } else {
            throw ServiceHelper.backendError(response);
        }
    }
}
