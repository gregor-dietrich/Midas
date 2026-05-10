package de.vptr.midas.api.resource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import de.vptr.midas.api.dto.PaymentDto;
import de.vptr.midas.api.dto.PaymentResponseDto;
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
import jakarta.ws.rs.BadRequestException;
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
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/** REST resource for managing payments. */
@Path("/payments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class PaymentResource {

    @Inject
    PaymentService paymentService;

    @Context
    @Nullable
    SecurityContext securityContext;

    /** Returns all payments with normalized pagination parameters. */
    @GET
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    public List<PaymentResponseDto> getAllPayments(@QueryParam("page") @DefaultValue("0") final int page,
            @QueryParam("size") @DefaultValue("20") final int size) {
        final int safePage = PaginationUtil.sanitizePage(page);
        final int safeSize = PaginationUtil.sanitizeSize(size);
        return this.paymentService.getAllPayments(safePage, safeSize);
    }

    /** Gets a payment by id. */
    @GET
    @Path("/{id}")
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    public Response getPayment(@PathParam("id") @Min(1) final Long id) {
        final String currentUsername = SecurityContextUtil.requireCurrentUsername(this.securityContext);
        return ResponseUtil.okOrNotFound(this.paymentService.findById(id, currentUsername));
    }

    @GET
    @Path("/user/{userId}")
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    public List<PaymentResponseDto> getPaymentsByUser(@PathParam("userId") @Min(1) final Long userId) {
        return this.paymentService.findByUserId(userId);
    }

    @GET
    @Path("/source/{sourceId}")
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    public List<PaymentResponseDto> getPaymentsBySourceAccount(@PathParam("sourceId") @Min(1) final Long sourceId) {
        return this.paymentService.findBySourceAccountId(sourceId);
    }

    @GET
    @Path("/target/{targetId}")
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    public List<PaymentResponseDto> getPaymentsByTargetAccount(@PathParam("targetId") @Min(1) final Long targetId) {
        return this.paymentService.findByTargetAccountId(targetId);
    }

    /** Returns all payments within the given date range. */
    @GET
    @Path("/date-range")
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    public List<PaymentResponseDto> getPaymentsByDateRange(@QueryParam("startDate") final LocalDate startDate,
            @QueryParam("endDate") final LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BadRequestException("Both startDate and endDate parameters are required");
        }
        return this.paymentService.findByDateRange(startDate, endDate);
    }

    @GET
    @Path("/recent")
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    public List<PaymentResponseDto> getRecentPayments(@QueryParam("limit") @DefaultValue("10") final int limit) {
        final int safeLimit = PaginationUtil.sanitizeRecentLimit(limit);
        return this.paymentService.findRecentPayments(safeLimit);
    }

    @GET
    @Path("/user/{userId}/total")
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    public Response getTotalAmountByUser(@PathParam("userId") @Min(1) final Long userId) {
        final BigDecimal total = this.paymentService.getTotalAmountByUser(userId);
        return ResponseUtil.ok(total);
    }

    @POST
    @RolesAllowed({ "user-account:add" })
    public Response createPayment(@Valid final PaymentDto paymentDto) {
        final PaymentResponseDto created = this.paymentService.createPayment(paymentDto);
        return ResponseUtil.created(created);
    }

    /** Updates the payment with the given id. */
    @PUT
    @Path("/{id}")
    @RolesAllowed({ "user-account:edit" })
    public Response updatePayment(@PathParam("id") @Min(1) final Long id, @Valid final PaymentDto paymentDto) {
        final String currentUsername = SecurityContextUtil.requireCurrentUsername(this.securityContext);
        final PaymentResponseDto updated = this.paymentService.updatePayment(id, paymentDto, currentUsername);
        return ResponseUtil.ok(updated);
    }

    /** Partially updates the payment with the given id. */
    @PATCH
    @Path("/{id}")
    @RolesAllowed({ "user-account:edit" })
    public Response patchPayment(@PathParam("id") @Min(1) final Long id, final PaymentDto paymentDto) {
        final String currentUsername = SecurityContextUtil.requireCurrentUsername(this.securityContext);
        final PaymentResponseDto updated = this.paymentService.patchPayment(id, paymentDto, currentUsername);
        return ResponseUtil.ok(updated);
    }

    /** Deletes the payment with the given id. */
    @DELETE
    @Path("/{id}")
    @RolesAllowed({ "user-account:delete" })
    public Response deletePayment(@PathParam("id") @Min(1) final Long id) {
        final String currentUsername = SecurityContextUtil.requireCurrentUsername(this.securityContext);
        final boolean deleted = this.paymentService.deletePayment(id, currentUsername);
        if (deleted) {
            return ResponseUtil.noContent();
        }
        return ResponseUtil.notFound();
    }

    @GET
    @Path("/search")
    @RolesAllowed({ "user-account:edit", "user-account:delete" })
    public List<PaymentResponseDto> searchPayments(@QueryParam("query") final String query) {
        return this.paymentService.searchPayments(query);
    }
}
