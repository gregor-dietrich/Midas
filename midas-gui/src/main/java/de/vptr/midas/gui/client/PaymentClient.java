package de.vptr.midas.gui.client;

import java.time.LocalDate;
import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import de.vptr.midas.gui.dto.PaymentDto;
import de.vptr.midas.gui.dto.PaymentResponseDto;
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
@Path("/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface PaymentClient {

        @GET
        List<PaymentResponseDto> getAllPayments(@QueryParam("page") Integer page, @QueryParam("size") Integer size,
                        @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/{id}")
        Response getPayment(@PathParam("id") Long id, @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/user/{userId}")
        List<PaymentResponseDto> getPaymentsByUser(@PathParam("userId") Long userId,
                        @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/source/{sourceId}")
        List<PaymentResponseDto> getPaymentsBySourceAccount(@PathParam("sourceId") Long sourceId,
                        @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/target/{targetId}")
        List<PaymentResponseDto> getPaymentsByTargetAccount(@PathParam("targetId") Long targetId,
                        @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/date-range")
        List<PaymentResponseDto> getPaymentsByDateRange(
                        @QueryParam("startDate") LocalDate startDate,
                        @QueryParam("endDate") LocalDate endDate,
                        @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/recent")
        List<PaymentResponseDto> getRecentPayments(@QueryParam("limit") int limit,
                        @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/user/{userId}/total")
        Response getTotalAmountByUser(@PathParam("userId") Long userId,
                        @HeaderParam("Authorization") String authorization);

        @POST
        Response createPayment(PaymentDto payment, @HeaderParam("Authorization") String authorization);

        @PUT
        @Path("/{id}")
        Response updatePayment(@PathParam("id") Long id, PaymentDto payment,
                        @HeaderParam("Authorization") String authorization);

        @PATCH
        @Path("/{id}")
        Response patchPayment(@PathParam("id") Long id, PaymentDto payment,
                        @HeaderParam("Authorization") String authorization);

        @DELETE
        @Path("/{id}")
        Response deletePayment(@PathParam("id") Long id, @HeaderParam("Authorization") String authorization);

        @GET
        @Path("/search")
        List<PaymentResponseDto> searchPayments(@QueryParam("query") String query,
                        @HeaderParam("Authorization") String authorization);
}
