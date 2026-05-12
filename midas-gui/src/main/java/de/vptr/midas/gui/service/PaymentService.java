package de.vptr.midas.gui.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import de.vptr.midas.gui.client.PaymentClient;
import de.vptr.midas.gui.dto.PaymentDto;
import de.vptr.midas.gui.dto.PaymentResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.util.PaginationParamUtil;
import de.vptr.midas.gui.util.ServiceHelper;
import de.vptr.midas.gui.util.UiConstants;
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
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public List<PaymentResponseDto> getAllPayments(final Integer page, final Integer size) {
        LOG.debug("Fetching all payments");
        final var authHeader = this.authService.requireAuthorizationHeader();
        final Integer safePage = PaginationParamUtil.sanitizePage(page);
        final Integer safeSize = PaginationParamUtil.sanitizeSize(size);
        return ServiceHelper.execute(() -> this.paymentClient.getAllPayments(safePage, safeSize, authHeader),
                this.authService, LOG, "fetching payments");
    }

    /** Returns the payment by id. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public Optional<PaymentDto> getPaymentById(final Long id) {
        LOG.debugf("Fetching payment with ID: %s", id);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.paymentClient.getPayment(id, authHeader), this.authService,
                LOG, "fetching payment " + id, response -> ServiceHelper.readOptionalEntity(response,
                        PaymentResponseDto.class, PaymentResponseDto::toPaymentDto, "payment response"));
    }

    /** Returns the payments by user. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public List<PaymentResponseDto> getPaymentsByUser(final Long userId) {
        LOG.debugf("Fetching payments for user: %s", userId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.paymentClient.getPaymentsByUser(userId, authHeader), this.authService,
                LOG, "fetching payments for user " + userId);
    }

    /** Returns the recent payments. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public List<PaymentResponseDto> getRecentPayments(final int limit) {
        final int safeLimit = PaginationParamUtil.sanitizeRecentLimit(limit);
        LOG.debugf("Fetching recent payments with limit: %s", safeLimit);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.paymentClient.getRecentPayments(safeLimit, authHeader),
                this.authService, LOG, "fetching recent payments");
    }

    /** Returns the payments by date range. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public List<PaymentResponseDto> getPaymentsByDateRange(final LocalDate startDate, final LocalDate endDate) {
        LOG.debugf("Fetching payments for date range: %s to %s", startDate, endDate);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.paymentClient.getPaymentsByDateRange(startDate, endDate, authHeader),
                this.authService, LOG, "fetching payments by date range");
    }

    /** Returns the payments by source account. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public List<PaymentResponseDto> getPaymentsBySourceAccount(final Long sourceId) {
        LOG.debugf("Fetching payments for source account: %s", sourceId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.paymentClient.getPaymentsBySourceAccount(sourceId, authHeader),
                this.authService, LOG, "fetching payments for source account " + sourceId);
    }

    /** Returns the payments by target account. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public List<PaymentResponseDto> getPaymentsByTargetAccount(final Long targetId) {
        LOG.debugf("Fetching payments for target account: %s", targetId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.paymentClient.getPaymentsByTargetAccount(targetId, authHeader),
                this.authService, LOG, "fetching payments for target account " + targetId);
    }

    /** Returns the payments by account (source or target). */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public List<PaymentResponseDto> getPaymentsByAccount(final Long accountId) {
        LOG.debugf("Fetching all payments for account: %s", accountId);
        final var authHeader = this.authService.requireAuthorizationHeader();

        final List<PaymentResponseDto> sourcePayments =
                ServiceHelper.execute(() -> this.paymentClient.getPaymentsBySourceAccount(accountId, authHeader),
                        this.authService, LOG, "fetching source payments for account " + accountId);

        final List<PaymentResponseDto> targetPayments =
                ServiceHelper.execute(() -> this.paymentClient.getPaymentsByTargetAccount(accountId, authHeader),
                        this.authService, LOG, "fetching target payments for account " + accountId);

        final Set<PaymentResponseDto> allPayments = new HashSet<>(sourcePayments);
        allPayments.addAll(targetPayments);

        return allPayments.stream().sorted((p1, p2) -> p2.getDate() != null ? p2.getDate().compareTo(p1.getDate()) : 0)
                .toList();
    }

    /** Creates a new payment. */
    public PaymentDto createPayment(final PaymentDto payment) {
        LOG.debugf("Creating new payment: %s", payment);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.paymentClient.createPayment(payment, authHeader),
                this.authService, LOG, "creating payment", "Failed to create payment",
                response -> ServiceHelper.readExpectedStatusEntity(response, 201, PaymentResponseDto.class,
                        PaymentResponseDto::toPaymentDto, "created payment response", "Failed to create payment"));
    }

    /** Updates the payment. */
    public PaymentDto updatePayment(final PaymentDto payment) {
        LOG.debugf("Updating payment: %s", payment);
        final var authHeader = this.authService.requireAuthorizationHeader();
        final var paymentId = Objects.requireNonNull(payment.getId(), "Payment ID must not be null for update");
        return ServiceHelper.executeWithResponse(() -> this.paymentClient.updatePayment(paymentId, payment, authHeader),
                this.authService, LOG, "updating payment " + payment.getId(), "Failed to update payment",
                response -> ServiceHelper.readExpectedStatusEntity(response, 200, PaymentResponseDto.class,
                        PaymentResponseDto::toPaymentDto, "updated payment response", "Failed to update payment"));
    }

    /** Handles the delete payment operation. */
    public boolean deletePayment(final Long id) {
        LOG.debugf("Deleting payment with ID: %s", id);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.paymentClient.deletePayment(id, authHeader),
                this.authService, LOG, "deleting payment " + id, response -> response.getStatus() == 204);
    }

    /** Handles the search payments operation. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS,
            abortOn = AuthenticationException.class)
    public List<PaymentResponseDto> searchPayments(final String query) {
        LOG.debugf("Searching payments with query: %s", query);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(() -> this.paymentClient.searchPayments(query, authHeader), this.authService, LOG,
                "searching payments");
    }
}
