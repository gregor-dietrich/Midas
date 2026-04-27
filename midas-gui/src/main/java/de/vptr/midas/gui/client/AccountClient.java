package de.vptr.midas.gui.client;

import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import de.vptr.midas.gui.dto.AccountDto;
import de.vptr.midas.gui.dto.AccountResponseDto;
import de.vptr.midas.gui.dto.PaymentDto;
import de.vptr.midas.gui.dto.PaymentResponseDto;
import de.vptr.midas.gui.dto.UserResponseDto;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
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
public interface AccountClient {

        @GET
        List<AccountResponseDto> getAllAccounts(@QueryParam("page") Integer page, @QueryParam("size") Integer size,
                        @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/{id}")
        Response getAccount(@PathParam("id") Long id, @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/name/{name}")
        Response getAccountByName(@PathParam("name") String name, @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/user/{userId}")
        List<AccountResponseDto> getAccountsByUser(@PathParam("userId") Long userId,
                        @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/{id}/payments/outgoing")
        Response getOutgoingPayments(@PathParam("id") Long id, @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/{id}/payments/incoming")
        Response getIncomingPayments(@PathParam("id") Long id, @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/{id}/users")
        List<UserResponseDto> getAssociatedUsers(@PathParam("id") Long id,
                        @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/search")
        List<AccountResponseDto> searchAccounts(@QueryParam("query") String query,
                        @HeaderParam("Authorization") String authorization);

        @POST
        Response createAccount(AccountDto account, @HeaderParam("Authorization") String authorization);

        @PUT
        @Path("/{id}")
        Response updateAccount(@PathParam("id") Long id, AccountDto account,
                        @HeaderParam("Authorization") String authorization);

        @DELETE
        @Path("/{id}")
        Response deleteAccount(@PathParam("id") Long id, @HeaderParam("Authorization") String authorization);

        @POST
        @Path("/{accountId}/users/{userId}")
        Response associateUserWithAccount(@PathParam("accountId") Long accountId, @PathParam("userId") Long userId,
                        @HeaderParam("Authorization") String authorization);

        @DELETE
        @Path("/{accountId}/users/{userId}")
        Response removeUserFromAccount(@PathParam("accountId") Long accountId, @PathParam("userId") Long userId,
                        @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/{id}/balance")
        Response getAccountBalance(@PathParam("id") Long id, @HeaderParam("Authorization") String authorization);

        // User-facing endpoints (only require authentication, no special roles)
        @GET
        @Path("/my")
        List<AccountResponseDto> getMyAccounts(@HeaderParam("Authorization") String authorization);

        @GET
        @Path("/my/with-balances")
        List<AccountResponseDto> getMyAccountsWithBalances(@HeaderParam("Authorization") String authorization);

        @GET
        @Path("/my/{id}/balance")
        Response getMyAccountBalance(@PathParam("id") Long id, @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/my/{accountId}/payments")
        List<PaymentResponseDto> getMyAccountPayments(@PathParam("accountId") Long accountId,
                        @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/my/{accountId}/payments/outgoing")
        List<PaymentResponseDto> getMyAccountOutgoingPayments(@PathParam("accountId") Long accountId,
                        @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/my/{accountId}/payments/incoming")
        List<PaymentResponseDto> getMyAccountIncomingPayments(@PathParam("accountId") Long accountId,
                        @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/my/{accountId}/check")
        Response checkMyAccountAccess(@PathParam("accountId") Long accountId,
                        @HeaderParam("Authorization") String authorization);

        @POST
        @Path("/my/{accountId}/payments")
        Response createMyAccountPayment(@PathParam("accountId") Long accountId, PaymentDto payment,
                        @HeaderParam("Authorization") String authorization);

        @PUT
        @Path("/my/{accountId}/payments/{paymentId}")
        Response updateMyAccountPayment(@PathParam("accountId") Long accountId, @PathParam("paymentId") Long paymentId,
                        PaymentDto payment, @HeaderParam("Authorization") String authorization);
}
