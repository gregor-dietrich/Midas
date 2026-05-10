package de.vptr.midas.api.resource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import de.vptr.midas.api.dto.AccountAccessResponseDto;
import de.vptr.midas.api.dto.AccountAssociationResponseDto;
import de.vptr.midas.api.dto.AccountDto;
import de.vptr.midas.api.dto.AccountResponseDto;
import de.vptr.midas.api.dto.PaymentDto;
import de.vptr.midas.api.dto.PaymentResponseDto;
import de.vptr.midas.api.dto.UserAccountMetaDto;
import de.vptr.midas.api.dto.UserResponseDto;
import de.vptr.midas.api.entity.UserEntity;
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
import jakarta.validation.constraints.Min;
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

    /** Returns the account with the given ID. */
    @GET
    @Path("/{id}")
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    public Response getAccount(@PathParam("id") @Min(1) final Long id) {
        final var account = this.accountService.findById(id);
        if (account.isPresent()) {
            return ResponseUtil.ok(account.get());
        }
        return ResponseUtil.notFound();
    }

    /** Returns the account with the given name. */
    @GET
    @Path("/name/{name}")
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    public Response getAccountByName(@PathParam("name") final String name) {
        final var account = this.accountService.findByName(name);
        if (account.isPresent()) {
            return ResponseUtil.ok(account.get());
        }
        return ResponseUtil.notFound();
    }

    /** Returns all users associated with the given account. */
    @GET
    @Path("/{id}/users")
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    public List<UserResponseDto> getAssociatedUsers(@PathParam("id") @Min(1) final Long accountId) {
        return this.accountAccessService.getAssociatedUsers(accountId);
    }

    /** Returns all payments for the given account. */
    @GET
    @Path("/{id}/payments")
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    public List<PaymentResponseDto> getAccountPayments(@PathParam("id") @Min(1) final Long accountId) {
        return this.accountAccessService.getAccountPayments(accountId);
    }

    /** Returns all outgoing payments for the given account. */
    @GET
    @Path("/{id}/payments/outgoing")
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    public List<PaymentResponseDto> getOutgoingPayments(@PathParam("id") @Min(1) final Long accountId) {
        return this.accountAccessService.getOutgoingPayments(accountId);
    }

    /** Returns all incoming payments for the given account. */
    @GET
    @Path("/{id}/payments/incoming")
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    public List<PaymentResponseDto> getIncomingPayments(@PathParam("id") @Min(1) final Long accountId) {
        return this.accountAccessService.getIncomingPayments(accountId);
    }

    /** Returns the balance for the given account. */
    @GET
    @Path("/{id}/balance")
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    public Response getAccountBalance(@PathParam("id") @Min(1) final Long accountId) {
        final BigDecimal balance = this.accountAccessService.getAccountBalance(accountId);
        return ResponseUtil.ok(Map.of("balance", balance));
    }

    /** Returns the balance for the account the current user has access to. */
    @GET
    @Path("/my/{id}/balance")
    public Response getMyAccountBalance(@PathParam("id") @Min(1) final Long accountId) {
        this.requireAccountAccess(accountId, "Access denied");
        final BigDecimal balance = this.accountAccessService.getAccountBalance(accountId);
        return ResponseUtil.ok(Map.of("balance", balance));
    }

    /** Returns all payments for the current user's account. */
    @GET
    @Path("/my/{accountId}/payments")
    public List<PaymentResponseDto> getMyAccountPayments(@PathParam("accountId") @Min(1) final Long accountId) {
        this.requireAccountAccess(accountId, "Access denied");
        return this.accountAccessService.getAccountPayments(accountId);
    }

    /** Returns all outgoing payments for the current user's account. */
    @GET
    @Path("/my/{accountId}/payments/outgoing")
    public List<PaymentResponseDto> getMyAccountOutgoingPayments(@PathParam("accountId") @Min(1) final Long accountId) {
        this.requireAccountAccess(accountId, "Access denied");
        return this.accountAccessService.getOutgoingPayments(accountId);
    }

    /** Returns all incoming payments for the current user's account. */
    @GET
    @Path("/my/{accountId}/payments/incoming")
    public List<PaymentResponseDto> getMyAccountIncomingPayments(@PathParam("accountId") @Min(1) final Long accountId) {
        this.requireAccountAccess(accountId, "Access denied");
        return this.accountAccessService.getIncomingPayments(accountId);
    }

    /** Returns all accounts for the given user. */
    @GET
    @Path("/user/{userId}")
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    public List<AccountResponseDto> getAccountsByUser(@PathParam("userId") @Min(1) final Long userId) {
        return this.accountAccessService.getAccountsForUser(userId);
    }

    /** Returns all accounts for the current user. */
    @GET
    @Path("/my")
    public List<AccountResponseDto> getMyAccounts() {
        final String username = this.requireCurrentUsername();
        return this.accountAccessService.getAccountsForCurrentUser(username);
    }

    /** Returns all accounts with normalized pagination and balance information. */
    @GET
    @Path("/my/with-balances")
    public List<AccountResponseDto> getMyAccountsWithBalances(@QueryParam("page") @DefaultValue("0") final int page,
            @QueryParam("size") @DefaultValue("20") final int size) {
        final String username = this.requireCurrentUsername();
        final int safePage = PaginationUtil.sanitizePage(page);
        final int safeSize = PaginationUtil.sanitizeSize(size);
        final List<AccountResponseDto> accounts =
                this.accountAccessService.getAccountsForCurrentUser(username, safePage, safeSize);

        if (accounts.isEmpty()) {
            return accounts;
        }

        final List<Long> accountIds = accounts.stream().map(account -> account.id).toList();

        final Map<Long, BigDecimal> balances = this.accountAccessService.getAccountBalances(accountIds);

        // Set balance in each account DTO
        accounts.forEach(account -> {
            account.balance = balances.getOrDefault(account.id, BigDecimal.ZERO);
        });

        // Set isNegative flag per account for the current user
        final UserEntity currentUser = UserEntity.find("username", username).firstResult();
        if (currentUser != null && currentUser.id != null) {
            final Map<Long, Boolean> isNegativeFlags =
                    this.accountAccessService.getIsNegativeFlagsForUser(currentUser.id, accountIds);
            accounts.forEach(account -> {
                account.isNegative = isNegativeFlags.getOrDefault(account.id, false);
            });
        }

        return accounts;
    }

    /** Checks whether the given user is associated with the given account. */
    @GET
    @Path("/user/{userId}/account/{accountId}/check")
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    public Response checkUserAccountAssociation(@PathParam("userId") @Min(1) final Long userId,
            @PathParam("accountId") @Min(1) final Long accountId) {
        final boolean isAssociated = this.accountAccessService.isUserAssociatedWithAccount(userId, accountId);
        return ResponseUtil.ok(new AccountAssociationResponseDto(isAssociated));
    }

    /** Checks whether the current user has access to the given account. */
    @GET
    @Path("/my/{accountId}/check")
    public Response checkMyAccountAccess(@PathParam("accountId") @Min(1) final Long accountId) {
        final String username = this.requireCurrentUsername();
        final boolean hasAccess = this.accountAccessService.hasUserAccessToAccount(username, accountId);
        return ResponseUtil.ok(new AccountAccessResponseDto(hasAccess));
    }

    /** Processes the request and returns the result. */
    @POST
    @RolesAllowed({ "user-account:add" })
    public Response createAccount(@Valid final AccountDto accountDto) {
        final var created = this.accountService.createAccount(accountDto);
        return ResponseUtil.created(created);
    }

    /** Processes the request and returns the result. */
    @PUT
    @Path("/{id}")
    @RolesAllowed({ "user-account:edit" })
    public Response updateAccount(@PathParam("id") @Min(1) final Long id, @Valid final AccountDto accountDto) {
        final var updated = this.accountService.updateAccount(id, accountDto);
        return ResponseUtil.ok(updated);
    }

    /** Processes the request and returns the result. */
    @PATCH
    @Path("/{id}")
    @RolesAllowed({ "user-account:edit" })
    public Response patchAccount(@PathParam("id") @Min(1) final Long id, final AccountDto accountDto) {
        final var updated = this.accountService.patchAccount(id, accountDto);
        return ResponseUtil.ok(updated);
    }

    /** Processes the request and returns the result. */
    @DELETE
    @Path("/{id}")
    @RolesAllowed({ "user-account:delete" })
    public Response deleteAccount(@PathParam("id") @Min(1) final Long id) {
        final boolean deleted = this.accountService.deleteAccount(id);
        if (deleted) {
            return ResponseUtil.noContent();
        }
        return ResponseUtil.notFound();
    }

    /** Associates a user with an account. */
    @POST
    @Path("/{accountId}/users/{userId}")
    @RolesAllowed({ "user-account:edit" })
    public Response associateUserWithAccount(@PathParam("accountId") @Min(1) final Long accountId,
            @PathParam("userId") @Min(1) final Long userId) {
        this.accountAccessService.associateUserWithAccount(userId, accountId);
        return ResponseUtil.created(null);
    }

    /** Processes the request and returns the result. */
    @DELETE
    @Path("/{accountId}/users/{userId}")
    @RolesAllowed({ "user-account:edit" })
    public Response removeUserFromAccount(@PathParam("accountId") @Min(1) final Long accountId,
            @PathParam("userId") @Min(1) final Long userId) {
        final boolean removed = this.accountAccessService.removeUserFromAccount(userId, accountId);
        if (removed) {
            return ResponseUtil.noContent();
        }
        return ResponseUtil.notFound();
    }

    /**
     * Updates the user-account meta (e.g. isNegative flag) for the association.
     *
     * @param accountId
     *            the account in the association
     * @param userId
     *            the user in the association
     * @param metaDto
     *            the meta properties to update
     * @return 204 No Content on success
     */
    @PATCH
    @Path("/{accountId}/users/{userId}")
    @RolesAllowed({ "user-account:edit" })
    public Response updateUserAccountMeta(@PathParam("accountId") @Min(1) final Long accountId,
            @PathParam("userId") @Min(1) final Long userId, final UserAccountMetaDto metaDto) {
        this.accountAccessService.updateUserAccountMeta(userId, accountId, metaDto.isNegative);
        return ResponseUtil.noContent();
    }

    /** Processes the request and returns the result. */
    @GET
    @Path("/search")
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    public Response searchAccounts(@QueryParam("query") final String query) {
        final var results = this.accountService.searchAccounts(query);
        return ResponseUtil.ok(results);
    }

    /** Processes the request and returns the result. */
    @POST
    @Path("/my/{accountId}/payments")
    public Response createMyAccountPayment(@PathParam("accountId") @Min(1) final Long accountId,
            @Valid final PaymentDto paymentDto) {
        final String username = this.requireCurrentUsername();
        final PaymentResponseDto created = this.paymentService.createPaymentForUser(paymentDto, username, accountId);
        return ResponseUtil.created(created);
    }

    /** Processes the request and returns the result. */
    @PUT
    @Path("/my/{accountId}/payments/{paymentId}")
    public Response updateMyAccountPayment(@PathParam("accountId") @Min(1) final Long accountId,
            @PathParam("paymentId") @Min(1) final Long paymentId, @Valid final PaymentDto paymentDto) {
        final String username = this.requireCurrentUsername();
        final PaymentResponseDto updated =
                this.paymentService.updatePaymentForUser(paymentId, paymentDto, username, accountId);
        return ResponseUtil.ok(updated);
    }

    private String requireCurrentUsername() {
        return SecurityContextUtil.requireCurrentUsername(this.securityContext);
    }

    private void requireAccountAccess(final Long accountId, final String deniedMessage) {
        final String username = this.requireCurrentUsername();
        if (!this.accountAccessService.hasUserAccessToAccount(username, accountId)) {
            throw new WebApplicationException(deniedMessage, Response.Status.FORBIDDEN);
        }
    }
}
