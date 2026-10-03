package de.vptr.midas.api.resource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import de.vptr.midas.api.dto.AccountAccessResponseDto;
import de.vptr.midas.api.dto.AccountAssociationResponseDto;
import de.vptr.midas.api.dto.AccountDto;
import de.vptr.midas.api.dto.AccountResponseDto;
import de.vptr.midas.api.dto.BalancePointDto;
import de.vptr.midas.api.dto.BalanceResponseDto;
import de.vptr.midas.api.dto.PaymentDto;
import de.vptr.midas.api.dto.PaymentResponseDto;
import de.vptr.midas.api.dto.UserAccountMetaDto;
import de.vptr.midas.api.dto.UserResponseDto;
import de.vptr.midas.api.entity.UserEntity;
import de.vptr.midas.api.exception.ApiErrorResponse;
import de.vptr.midas.api.service.AccountAccessService;
import de.vptr.midas.api.service.AccountService;
import de.vptr.midas.api.service.PaymentService;
import de.vptr.midas.api.util.PaginationUtil;
import de.vptr.midas.api.util.ResponseUtil;
import de.vptr.midas.api.util.SecurityContextUtil;
import io.quarkus.security.Authenticated;
import jakarta.annotation.Nullable;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/** REST resource for managing accounts. */
@Path("/accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Account")
public class AccountResource {

    @Inject
    AccountAccessService accountAccessService;

    @Inject
    AccountService accountService;

    @Inject
    PaymentService paymentService;

    @Context
    @Nullable
    SecurityContext securityContext;

    /** Returns all accounts with normalized pagination parameters. */
    @GET
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    public List<AccountResponseDto> getAllAccounts(@QueryParam("page") @DefaultValue("0") final int page,
            @QueryParam("size") @DefaultValue("20") final int size) {
        final int safePage = PaginationUtil.sanitizePage(page);
        final int safeSize = PaginationUtil.sanitizeSize(size);
        return this.accountService.getAllAccounts(safePage, safeSize);
    }

    /** Returns the account with the given publicId. */
    @GET
    @Path("/{publicId}")
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    @APIResponseSchema(value = AccountResponseDto.class, responseDescription = "Account found")
    @APIResponse(responseCode = "404", description = "Account not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response getAccount(@PathParam("publicId") final String publicId) {
        final var account = this.accountService.findById(publicId);
        if (account.isPresent()) {
            return ResponseUtil.ok(account.get());
        }
        return ResponseUtil.notFound();
    }

    /** Returns the account with the given name. */
    @GET
    @Path("/name/{name}")
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    @APIResponseSchema(value = AccountResponseDto.class, responseDescription = "Account found")
    @APIResponse(responseCode = "404", description = "Account not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response getAccountByName(@PathParam("name") final String name) {
        final var account = this.accountService.findByName(name);
        if (account.isPresent()) {
            return ResponseUtil.ok(account.get());
        }
        return ResponseUtil.notFound();
    }

    /** Returns all users associated with the given account. */
    @GET
    @Path("/{publicId}/users")
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    public List<UserResponseDto> getAssociatedUsers(@PathParam("publicId") final String publicId) {
        return this.accountAccessService.getAssociatedUsers(publicId);
    }

    /** Returns all payments for the given account. */
    @GET
    @Path("/{publicId}/payments")
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    public List<PaymentResponseDto> getAccountPayments(@PathParam("publicId") final String publicId) {
        return this.accountAccessService.getAccountPayments(publicId);
    }

    /** Returns all outgoing payments for the given account. */
    @GET
    @Path("/{publicId}/payments/outgoing")
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    public List<PaymentResponseDto> getOutgoingPayments(@PathParam("publicId") final String publicId) {
        return this.accountAccessService.getOutgoingPayments(publicId);
    }

    /** Returns all incoming payments for the given account. */
    @GET
    @Path("/{publicId}/payments/incoming")
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    public List<PaymentResponseDto> getIncomingPayments(@PathParam("publicId") final String publicId) {
        return this.accountAccessService.getIncomingPayments(publicId);
    }

    /** Returns the balance for the given account. */
    @GET
    @Path("/{publicId}/balance")
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    @APIResponseSchema(value = BalanceResponseDto.class, responseDescription = "Account balance")
    public Response getAccountBalance(@PathParam("publicId") final String publicId) {
        final BigDecimal balance = this.accountAccessService.getAccountBalance(publicId);
        return ResponseUtil.ok(new BalanceResponseDto(balance));
    }

    /**
     * Returns the balance for the account the current user has access to. When both {@code startDate} and
     * {@code endDate} are provided the balance is restricted to that inclusive date range.
     */
    @GET
    @Path("/my/{publicId}/balance")
    @APIResponseSchema(value = BalanceResponseDto.class, responseDescription = "Account balance")
    @APIResponse(responseCode = "403", description = "Caller has no access to this account",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response getMyAccountBalance(@PathParam("publicId") final String publicId,
            @QueryParam("startDate") @Nullable final LocalDate startDate,
            @QueryParam("endDate") @Nullable final LocalDate endDate) {
        this.requireAccountAccess(publicId, "Access denied");
        final BigDecimal balance = this.accountAccessService.getAccountBalance(publicId, startDate, endDate);
        return ResponseUtil.ok(new BalanceResponseDto(balance));
    }

    /**
     * Returns the cumulative balance-over-time series for the account the current user has access to. When both
     * {@code startDate} and {@code endDate} are provided the series is restricted to that inclusive date range and
     * starts at zero at the range start.
     */
    @GET
    @Path("/my/{publicId}/balance-history")
    public List<BalancePointDto> getMyAccountBalanceHistory(@PathParam("publicId") final String publicId,
            @QueryParam("startDate") @Nullable final LocalDate startDate,
            @QueryParam("endDate") @Nullable final LocalDate endDate) {
        this.requireAccountAccess(publicId, "Access denied");
        return this.accountAccessService.getAccountBalanceHistory(publicId, startDate, endDate);
    }

    /**
     * Returns the cumulative total balance-over-time series across all of the current user's accounts, signed by each
     * account's per-user {@code isNegative} flag. When both {@code startDate} and {@code endDate} are provided the
     * series is restricted to that inclusive date range and starts at zero at the range start.
     */
    @GET
    @Path("/my/balance-history")
    public List<BalancePointDto> getMyTotalBalanceHistory(@QueryParam("startDate") @Nullable final LocalDate startDate,
            @QueryParam("endDate") @Nullable final LocalDate endDate) {
        final String username = this.requireCurrentUsername();
        return this.accountAccessService.getTotalBalanceHistory(username, startDate, endDate);
    }

    /**
     * Returns a single page of payments for the current user's account, with optional server-side date-range and
     * free-text filtering. Pagination is mandatory so the full payment set is never materialized client-side.
     */
    @GET
    @Path("/my/{publicId}/payments")
    public List<PaymentResponseDto> getMyAccountPayments(@PathParam("publicId") final String publicId,
            @QueryParam("page") @DefaultValue("0") final int page,
            @QueryParam("size") @DefaultValue("20") final int size,
            @QueryParam("startDate") @Nullable final LocalDate startDate,
            @QueryParam("endDate") @Nullable final LocalDate endDate,
            @QueryParam("query") @Nullable final String query) {
        this.requireAccountAccess(publicId, "Access denied");
        final int safePage = PaginationUtil.sanitizePage(page);
        final int safeSize = PaginationUtil.sanitizeSize(size);
        return this.accountAccessService.getAccountPayments(publicId, startDate, endDate, query, safePage, safeSize);
    }

    /** Returns payments grouped by account for all accounts of the current user. */
    @GET
    @Path("/my/payments")
    public Map<String, List<PaymentResponseDto>> getMyAccountsPayments() {
        final String username = this.requireCurrentUsername();
        final List<AccountResponseDto> accounts = this.accountAccessService.getAccountsForCurrentUser(username);
        if (accounts.isEmpty()) {
            return Map.of();
        }
        final List<String> publicIds = accounts.stream().map(a -> a.publicId).toList();
        return this.accountAccessService.getPaymentsByAccount(publicIds);
    }

    /** Returns all outgoing payments for the current user's account. */
    @GET
    @Path("/my/{publicId}/payments/outgoing")
    public List<PaymentResponseDto> getMyAccountOutgoingPayments(@PathParam("publicId") final String publicId) {
        this.requireAccountAccess(publicId, "Access denied");
        return this.accountAccessService.getOutgoingPayments(publicId);
    }

    /** Returns all incoming payments for the current user's account. */
    @GET
    @Path("/my/{publicId}/payments/incoming")
    public List<PaymentResponseDto> getMyAccountIncomingPayments(@PathParam("publicId") final String publicId) {
        this.requireAccountAccess(publicId, "Access denied");
        return this.accountAccessService.getIncomingPayments(publicId);
    }

    /** Returns all accounts for the given user. */
    @GET
    @Path("/user/{userPublicId}")
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    public List<AccountResponseDto> getAccountsByUser(@PathParam("userPublicId") final String userPublicId) {
        return this.accountAccessService.getAccountsForUser(userPublicId);
    }

    /** Returns all accounts for the current user. */
    @GET
    @Path("/my")
    public List<AccountResponseDto> getMyAccounts() {
        final String username = this.requireCurrentUsername();
        final List<AccountResponseDto> accounts = this.accountAccessService.getAccountsForCurrentUser(username);
        this.populateAccountMetaFlags(username, accounts);
        return accounts;
    }

    /**
     * Returns all accounts with normalized pagination and balance information. When both {@code startDate} and
     * {@code endDate} are provided each account's balance is restricted to that inclusive date range, computed
     * server-side so the GUI never loads the underlying payments.
     */
    @GET
    @Path("/my/with-balances")
    public List<AccountResponseDto> getMyAccountsWithBalances(@QueryParam("page") @DefaultValue("0") final int page,
            @QueryParam("size") @DefaultValue("20") final int size,
            @QueryParam("startDate") @Nullable final LocalDate startDate,
            @QueryParam("endDate") @Nullable final LocalDate endDate) {
        final String username = this.requireCurrentUsername();
        final int safePage = PaginationUtil.sanitizePage(page);
        final int safeSize = PaginationUtil.sanitizeSize(size);
        final List<AccountResponseDto> accounts =
                this.accountAccessService.getAccountsForCurrentUser(username, safePage, safeSize);

        if (accounts.isEmpty()) {
            return accounts;
        }

        final List<String> accountPublicIds = accounts.stream().map(account -> account.publicId).toList();

        final Map<String, BigDecimal> balances =
                this.accountAccessService.getAccountBalances(accountPublicIds, startDate, endDate);

        // Set balance in each account DTO
        accounts.forEach(account -> {
            account.balance = balances.getOrDefault(account.publicId, BigDecimal.ZERO);
        });

        this.populateAccountMetaFlags(username, accounts);

        return accounts;
    }

    private void populateAccountMetaFlags(final String username, final List<AccountResponseDto> accounts) {
        if (accounts.isEmpty()) {
            return;
        }

        final UserEntity currentUser = UserEntity.find("username", username).firstResult();
        if (currentUser != null && currentUser.id != null) {
            final List<String> accountPublicIds = accounts.stream().map(account -> account.publicId).toList();
            final Map<String, Boolean> negativeFlagsByAccount =
                    this.accountAccessService.getIsNegativeFlagsForUser(currentUser.id, accountPublicIds);
            final Map<String, Boolean> manageFlagsByAccount =
                    this.accountAccessService.getCanManageFlagsForUser(currentUser.id, accountPublicIds);
            accounts.forEach(account -> {
                account.isNegative = negativeFlagsByAccount.getOrDefault(account.publicId, false);
                account.canManage = manageFlagsByAccount.getOrDefault(account.publicId, false);
            });
        }
    }

    /** Checks whether the given user is associated with the given account. */
    @GET
    @Path("/user/{userPublicId}/account/{accountPublicId}/check")
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    @APIResponseSchema(value = AccountAssociationResponseDto.class, responseDescription = "Association status")
    public Response checkUserAccountAssociation(@PathParam("userPublicId") final String userPublicId,
            @PathParam("accountPublicId") final String accountPublicId) {
        final boolean isAssociated =
                this.accountAccessService.isUserAssociatedWithAccount(userPublicId, accountPublicId);
        return ResponseUtil.ok(new AccountAssociationResponseDto(isAssociated));
    }

    /** Checks whether the current user has access to the given account. */
    @GET
    @Path("/my/{publicId}/check")
    @APIResponseSchema(value = AccountAccessResponseDto.class, responseDescription = "Access status")
    public Response checkMyAccountAccess(@PathParam("publicId") final String publicId) {
        final String username = this.requireCurrentUsername();
        final boolean hasAccess = this.accountAccessService.hasUserAccessToAccount(username, publicId);
        return ResponseUtil.ok(new AccountAccessResponseDto(hasAccess));
    }

    /** Processes the request and returns the result. */
    @POST
    @RolesAllowed({ "user-account:add" })
    @APIResponseSchema(value = AccountResponseDto.class, responseCode = "201", responseDescription = "Account created")
    @APIResponse(responseCode = "422", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response createAccount(@Valid final AccountDto accountDto) {
        final var created = this.accountService.createAccount(accountDto);
        return ResponseUtil.created(created);
    }

    /** Processes the request and returns the result. */
    @PUT
    @Path("/{publicId}")
    @RolesAllowed({ "user-account:edit" })
    @APIResponseSchema(value = AccountResponseDto.class, responseDescription = "Account replaced")
    @APIResponse(responseCode = "404", description = "Account not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "422", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response updateAccount(@PathParam("publicId") final String publicId, @Valid final AccountDto accountDto) {
        final var updated = this.accountService.updateAccount(publicId, accountDto);
        return ResponseUtil.ok(updated);
    }

    /** Processes the request and returns the result. */
    @PATCH
    @Path("/{publicId}")
    @RolesAllowed({ "user-account:edit" })
    @APIResponseSchema(value = AccountResponseDto.class, responseDescription = "Account updated")
    @APIResponse(responseCode = "404", description = "Account not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response patchAccount(@PathParam("publicId") final String publicId, final AccountDto accountDto) {
        final var updated = this.accountService.patchAccount(publicId, accountDto);
        return ResponseUtil.ok(updated);
    }

    /** Processes the request and returns the result. */
    @DELETE
    @Path("/{publicId}")
    @RolesAllowed({ "user-account:delete" })
    @APIResponse(responseCode = "204", description = "Account deleted")
    @APIResponse(responseCode = "404", description = "Account not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "409", description = "Account still has payments",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response deleteAccount(@PathParam("publicId") final String publicId) {
        final boolean deleted = this.accountService.deleteAccount(publicId);
        if (deleted) {
            return ResponseUtil.noContent();
        }
        return ResponseUtil.notFound();
    }

    /** Associates a user with an account. */
    @POST
    @Path("/{accountPublicId}/users/{userPublicId}")
    @RolesAllowed({ "user-account:edit" })
    @APIResponse(responseCode = "201", description = "User associated with account")
    @APIResponse(responseCode = "404", description = "User or account not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "409", description = "User is already associated with this account",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response associateUserWithAccount(@PathParam("accountPublicId") final String accountPublicId,
            @PathParam("userPublicId") final String userPublicId) {
        this.accountAccessService.associateUserWithAccount(userPublicId, accountPublicId);
        return ResponseUtil.created(null);
    }

    /** Processes the request and returns the result. */
    @DELETE
    @Path("/{accountPublicId}/users/{userPublicId}")
    @RolesAllowed({ "user-account:edit" })
    @APIResponse(responseCode = "204", description = "Association removed")
    @APIResponse(responseCode = "404", description = "Association not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response removeUserFromAccount(@PathParam("accountPublicId") final String accountPublicId,
            @PathParam("userPublicId") final String userPublicId) {
        final boolean removed = this.accountAccessService.removeUserFromAccount(userPublicId, accountPublicId);
        if (removed) {
            return ResponseUtil.noContent();
        }
        return ResponseUtil.notFound();
    }

    /**
     * Updates the user-account meta (e.g. isNegative flag) for the association.
     *
     * @param accountPublicId
     *            the account in the association
     * @param userPublicId
     *            the user in the association
     * @param metaDto
     *            the meta properties to update
     * @return 204 No Content on success
     */
    @PATCH
    @Path("/{accountPublicId}/users/{userPublicId}")
    @RolesAllowed({ "user-account:edit" })
    @APIResponse(responseCode = "204", description = "Association meta updated")
    @APIResponse(responseCode = "404", description = "Association not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response updateUserAccountMeta(@PathParam("accountPublicId") final String accountPublicId,
            @PathParam("userPublicId") final String userPublicId, final UserAccountMetaDto metaDto) {
        this.accountAccessService.updateUserAccountMeta(userPublicId, accountPublicId, metaDto.isNegative,
                metaDto.canManage);
        return ResponseUtil.noContent();
    }

    /** Processes the request and returns the result. */
    @GET
    @Path("/search")
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    @APIResponseSchema(value = AccountResponseDto[].class, responseDescription = "Matching accounts")
    public Response searchAccounts(@QueryParam("query") final String query) {
        final var results = this.accountService.searchAccounts(query);
        return ResponseUtil.ok(results);
    }

    /** Processes the request and returns the result. */
    @POST
    @Path("/my/{accountPublicId}/payments")
    @APIResponseSchema(value = PaymentResponseDto.class, responseCode = "201", responseDescription = "Payment created")
    @APIResponse(responseCode = "403", description = "Caller has no access to this account",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "422", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response createMyAccountPayment(@PathParam("accountPublicId") final String accountPublicId,
            @Valid final PaymentDto paymentDto) {
        final String username = this.requireCurrentUsername();
        final PaymentResponseDto created =
                this.paymentService.createPaymentForUser(paymentDto, username, accountPublicId);
        return ResponseUtil.created(created);
    }

    /** Processes the request and returns the result. */
    @PUT
    @Path("/my/{accountPublicId}/payments/{paymentPublicId}")
    @APIResponseSchema(value = PaymentResponseDto.class, responseDescription = "Payment replaced")
    @APIResponse(responseCode = "404", description = "Payment not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "403", description = "Caller has no access to this account",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "422", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response updateMyAccountPayment(@PathParam("accountPublicId") final String accountPublicId,
            @PathParam("paymentPublicId") final String paymentPublicId, @Valid final PaymentDto paymentDto) {
        final String username = this.requireCurrentUsername();
        final PaymentResponseDto updated =
                this.paymentService.updatePaymentForUser(paymentPublicId, paymentDto, username, accountPublicId);
        return ResponseUtil.ok(updated);
    }

    /** Processes the request and returns the result. */
    @DELETE
    @Path("/my/{accountPublicId}/payments/{paymentPublicId}")
    @APIResponse(responseCode = "204", description = "Payment deleted")
    @APIResponse(responseCode = "404", description = "Payment not found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "403", description = "Caller has no access to this account",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response deleteMyAccountPayment(@PathParam("accountPublicId") final String accountPublicId,
            @PathParam("paymentPublicId") final String paymentPublicId) {
        final String username = this.requireCurrentUsername();
        final boolean deleted = this.paymentService.deletePaymentForUser(paymentPublicId, username, accountPublicId);
        if (deleted) {
            return ResponseUtil.noContent();
        }
        return ResponseUtil.notFound();
    }

    private String requireCurrentUsername() {
        return SecurityContextUtil.requireCurrentUsername(this.securityContext);
    }

    private void requireAccountAccess(final String accountPublicId, final String deniedMessage) {
        final String username = this.requireCurrentUsername();
        if (!this.accountAccessService.hasUserAccessToAccount(username, accountPublicId)) {
            throw new WebApplicationException(deniedMessage, Response.Status.FORBIDDEN);
        }
    }
}
