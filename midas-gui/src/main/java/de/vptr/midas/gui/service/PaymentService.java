package de.vptr.midas.gui.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import de.vptr.midas.gui.client.PaymentClient;
import de.vptr.midas.gui.dto.PaymentDto;
import de.vptr.midas.gui.dto.PaymentResponseDto;
import de.vptr.midas.gui.util.PaginationParamUtil;
import de.vptr.midas.gui.util.ServiceHelper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Service for payment operations. */
@ApplicationScoped
public class PaymentService {

    private static final Logger LOG = Logger.getLogger(PaymentService.class);

    @Inject
    @RestClient
    PaymentClient paymentClient;

    @Inject
    AuthService authService;

    /** Returns the all payments. */
    public List<PaymentResponseDto> getAllPayments(final Integer page, final Integer size) {
        LOG.debug("Fetching all payments");
        final Integer safePage = PaginationParamUtil.sanitizePage(page);
        final Integer safeSize = PaginationParamUtil.sanitizeSize(size);
        return ServiceHelper.execute(() -> this.paymentClient.getAllPayments(safePage, safeSize), this.authService, LOG,
                "fetching payments");
    }

    /** Returns the payment by id, or empty if it does not exist. */
    public Optional<PaymentDto> getPaymentById(final String id) {
        LOG.debugf("Fetching payment with ID: %s", id);
        return ServiceHelper.executeOptional(() -> this.paymentClient.getPayment(id), this.authService, LOG,
                "fetching payment " + id).map(PaymentDto::from);
    }

    /** Returns the payments by user. */
    public List<PaymentResponseDto> getPaymentsByUser(final String userId) {
        LOG.debugf("Fetching payments for user: %s", userId);
        return ServiceHelper.execute(() -> this.paymentClient.getPaymentsByUser(userId), this.authService, LOG,
                "fetching payments for user " + userId);
    }

    /** Returns the recent payments. */
    public List<PaymentResponseDto> getRecentPayments(final int limit) {
        final int safeLimit = PaginationParamUtil.sanitizeRecentLimit(limit);
        LOG.debugf("Fetching recent payments with limit: %s", safeLimit);
        return ServiceHelper.execute(() -> this.paymentClient.getRecentPayments(safeLimit), this.authService, LOG,
                "fetching recent payments");
    }

    /** Returns the payments by date range. */
    public List<PaymentResponseDto> getPaymentsByDateRange(final LocalDate startDate, final LocalDate endDate) {
        LOG.debugf("Fetching payments for date range: %s to %s", startDate, endDate);
        return ServiceHelper.execute(() -> this.paymentClient.getPaymentsByDateRange(endDate, startDate),
                this.authService, LOG, "fetching payments by date range");
    }

    /** Returns the payments by source account. */
    public List<PaymentResponseDto> getPaymentsBySourceAccount(final String sourceId) {
        LOG.debugf("Fetching payments for source account: %s", sourceId);
        return ServiceHelper.execute(() -> this.paymentClient.getPaymentsBySourceAccount(sourceId), this.authService,
                LOG, "fetching payments for source account " + sourceId);
    }

    /** Returns the payments by target account. */
    public List<PaymentResponseDto> getPaymentsByTargetAccount(final String targetId) {
        LOG.debugf("Fetching payments for target account: %s", targetId);
        return ServiceHelper.execute(() -> this.paymentClient.getPaymentsByTargetAccount(targetId), this.authService,
                LOG, "fetching payments for target account " + targetId);
    }

    /** Returns the payments by account (source or target). */
    public List<PaymentResponseDto> getPaymentsByAccount(final String accountId) {
        LOG.debugf("Fetching all payments for account: %s", accountId);

        final List<PaymentResponseDto> sourcePayments =
                ServiceHelper.execute(() -> this.paymentClient.getPaymentsBySourceAccount(accountId), this.authService,
                        LOG, "fetching source payments for account " + accountId);

        final List<PaymentResponseDto> targetPayments =
                ServiceHelper.execute(() -> this.paymentClient.getPaymentsByTargetAccount(accountId), this.authService,
                        LOG, "fetching target payments for account " + accountId);

        final Set<PaymentResponseDto> allPayments = new HashSet<>(sourcePayments);
        allPayments.addAll(targetPayments);

        return allPayments.stream().sorted((p1, p2) -> p2.getDate() != null ? p2.getDate().compareTo(p1.getDate()) : 0)
                .toList();
    }

    /** Creates a new payment. */
    public PaymentDto createPayment(final PaymentDto payment) {
        LOG.debugf("Creating new payment: %s", payment);
        return PaymentDto.from(ServiceHelper.execute(() -> this.paymentClient.createPayment(payment), this.authService,
                LOG, "creating payment", "Failed to create payment"));
    }

    /** Updates the payment. */
    public PaymentDto updatePayment(final PaymentDto payment) {
        LOG.debugf("Updating payment: %s", payment);
        final var paymentId = Objects.requireNonNull(payment.getPublicId(), "Payment ID must not be null for update");
        return PaymentDto.from(ServiceHelper.execute(() -> this.paymentClient.updatePayment(paymentId, payment),
                this.authService, LOG, "updating payment " + payment.getPublicId(), "Failed to update payment"));
    }

    /** Handles the delete payment operation. */
    public void deletePayment(final String id) {
        LOG.debugf("Deleting payment with ID: %s", id);
        ServiceHelper.executeVoid(() -> this.paymentClient.deletePayment(id), this.authService, LOG,
                "deleting payment " + id);
    }

    /** Handles the search payments operation. */
    public List<PaymentResponseDto> searchPayments(final String query) {
        LOG.debugf("Searching payments with query: %s", query);
        return ServiceHelper.execute(() -> this.paymentClient.searchPayments(query), this.authService, LOG,
                "searching payments");
    }
}
