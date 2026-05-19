package de.vptr.midas.gui.client;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import de.vptr.midas.gui.dto.AccountDto;
import de.vptr.midas.gui.dto.AccountResponseDto;
import de.vptr.midas.gui.dto.PaymentDto;
import de.vptr.midas.gui.dto.PaymentResponseDto;
import de.vptr.midas.gui.dto.UserAccountMetaDto;
import de.vptr.midas.gui.dto.UserResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.util.UiConstants;
import jakarta.annotation.Nullable;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Component. */
@RegisterRestClient(configKey = "midas-api")
@Path("/accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
        abortOn = AuthenticationException.class)
@Timeout(2000)
public interface AccountClient {

    @GET
    List<AccountResponseDto> getAllAccounts(@QueryParam("page") Integer page, @QueryParam("size") Integer size,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/{publicId}")
    Response getAccount(@PathParam("publicId") String publicId, @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/name/{name}")
    Response getAccountByName(@PathParam("name") String name, @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/user/{userPublicId}")
    List<AccountResponseDto> getAccountsByUser(@PathParam("userPublicId") String userPublicId,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/{publicId}/payments/outgoing")
    Response getOutgoingPayments(@PathParam("publicId") String publicId,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/{publicId}/payments/incoming")
    Response getIncomingPayments(@PathParam("publicId") String publicId,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/{publicId}/users")
    List<UserResponseDto> getAssociatedUsers(@PathParam("publicId") String publicId,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/search")
    List<AccountResponseDto> searchAccounts(@QueryParam("query") String query,
            @HeaderParam("Authorization") String authorization);

    @POST
    Response createAccount(AccountDto account, @HeaderParam("Authorization") String authorization);

    @PUT
    @Path("/{publicId}")
    Response updateAccount(@PathParam("publicId") String publicId, AccountDto account,
            @HeaderParam("Authorization") String authorization);

    @DELETE
    @Path("/{publicId}")
    Response deleteAccount(@PathParam("publicId") String publicId, @HeaderParam("Authorization") String authorization);

    @POST
    @Path("/{accountPublicId}/users/{userPublicId}")
    Response associateUserWithAccount(@PathParam("accountPublicId") String accountPublicId,
            @PathParam("userPublicId") String userPublicId, @HeaderParam("Authorization") String authorization);

    @DELETE
    @Path("/{accountPublicId}/users/{userPublicId}")
    Response removeUserFromAccount(@PathParam("accountPublicId") String accountPublicId,
            @PathParam("userPublicId") String userPublicId, @HeaderParam("Authorization") String authorization);

    @PATCH
    @Path("/{accountPublicId}/users/{userPublicId}")
    Response updateUserAccountMeta(@PathParam("accountPublicId") String accountPublicId,
            @PathParam("userPublicId") String userPublicId, UserAccountMetaDto metaDto,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/{publicId}/balance")
    Response getAccountBalance(@PathParam("publicId") String publicId,
            @HeaderParam("Authorization") String authorization);

    // User-facing endpoints (only require authentication, no special roles)
    @GET
    @Path("/my")
    List<AccountResponseDto> getMyAccounts(@HeaderParam("Authorization") String authorization);

    @GET
    @Path("/my/with-balances")
    List<AccountResponseDto> getMyAccountsWithBalances(@QueryParam("page") Integer page,
            @QueryParam("size") Integer size, @QueryParam("startDate") @Nullable LocalDate startDate,
            @QueryParam("endDate") @Nullable LocalDate endDate, @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/my/{publicId}/balance")
    Response getMyAccountBalance(@PathParam("publicId") String publicId,
            @QueryParam("startDate") @Nullable LocalDate startDate, @QueryParam("endDate") @Nullable LocalDate endDate,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/my/{accountPublicId}/payments")
    List<PaymentResponseDto> getMyAccountPayments(@PathParam("accountPublicId") String accountPublicId,
            @QueryParam("page") Integer page, @QueryParam("size") Integer size,
            @QueryParam("startDate") @Nullable LocalDate startDate, @QueryParam("endDate") @Nullable LocalDate endDate,
            @QueryParam("query") @Nullable String query, @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/my/payments")
    Map<String, List<PaymentResponseDto>> getMyAccountsPayments(@HeaderParam("Authorization") String authorization);

    @GET
    @Path("/my/{accountPublicId}/payments/outgoing")
    List<PaymentResponseDto> getMyAccountOutgoingPayments(@PathParam("accountPublicId") String accountPublicId,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/my/{accountPublicId}/payments/incoming")
    List<PaymentResponseDto> getMyAccountIncomingPayments(@PathParam("accountPublicId") String accountPublicId,
            @HeaderParam("Authorization") String authorization);

    @GET
    @Path("/my/{accountPublicId}/check")
    Response checkMyAccountAccess(@PathParam("accountPublicId") String accountPublicId,
            @HeaderParam("Authorization") String authorization);

    @POST
    @Path("/my/{accountPublicId}/payments")
    Response createMyAccountPayment(@PathParam("accountPublicId") String accountPublicId, PaymentDto payment,
            @HeaderParam("Authorization") String authorization);

    @PUT
    @Path("/my/{accountPublicId}/payments/{paymentPublicId}")
    Response updateMyAccountPayment(@PathParam("accountPublicId") String accountPublicId,
            @PathParam("paymentPublicId") String paymentPublicId, PaymentDto payment,
            @HeaderParam("Authorization") String authorization);

    @DELETE
    @Path("/my/{accountPublicId}/payments/{paymentPublicId}")
    Response deleteMyAccountPayment(@PathParam("accountPublicId") String accountPublicId,
            @PathParam("paymentPublicId") String paymentPublicId, @HeaderParam("Authorization") String authorization);
}
