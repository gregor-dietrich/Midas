package de.vptr.midas.gui.service;

import de.vptr.midas.gui.util.UiConstants;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import de.vptr.midas.gui.client.PaymentClient;
import de.vptr.midas.gui.dto.PaymentDto;
import de.vptr.midas.gui.dto.PaymentResponseDto;
import de.vptr.midas.gui.exception.AuthenticationException;
import de.vptr.midas.gui.exception.ServiceException;
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
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public List<PaymentResponseDto> getAllPayments(final Integer page, final Integer size)
            throws ServiceException, AuthenticationException {
        LOG.debug("Fetching all payments");
        final var authHeader = this.authService.requireAuthorizationHeader();
        final Integer safePage = PaginationParamUtil.sanitizePage(page);
        final Integer safeSize = PaginationParamUtil.sanitizeSize(size);
        return ServiceHelper.execute(
                () -> this.paymentClient.getAllPayments(safePage, safeSize, authHeader),
                this.authService, LOG, "fetching payments");
    }

    /** Returns the payment by id. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public Optional<PaymentDto> getPaymentById(final Long id) {
        LOG.debugf("Fetching payment with ID: %s", id);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.paymentClient.getPayment(id, authHeader),
                this.authService, LOG, "fetching payment " + id, response -> {
                    if (response.getStatus() == 200) {
                        final PaymentResponseDto responseDto = ServiceHelper.readEntity(response,
                                PaymentResponseDto.class,
                                "payment response");
                        return Optional.of(responseDto.toPaymentDto());
                    } else if (response.getStatus() == 404) {
                        return Optional.empty();
                    } else {
                        throw ServiceHelper.backendError(response);
                    }
                });
    }

    /** Returns the payments by user. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public List<PaymentResponseDto> getPaymentsByUser(final Long userId) {
        LOG.debugf("Fetching payments for user: %s", userId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(
                () -> this.paymentClient.getPaymentsByUser(userId, authHeader),
                this.authService, LOG, "fetching payments for user " + userId);
    }

    /** Returns the recent payments. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public List<PaymentResponseDto> getRecentPayments(final int limit) {
        final int safeLimit = PaginationParamUtil.sanitizeRecentLimit(limit);
        LOG.debugf("Fetching recent payments with limit: %s", safeLimit);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(
                () -> this.paymentClient.getRecentPayments(safeLimit, authHeader),
                this.authService, LOG, "fetching recent payments");
    }

    /** Returns the payments by date range. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public List<PaymentResponseDto> getPaymentsByDateRange(final LocalDate startDate, final LocalDate endDate) {
        LOG.debugf("Fetching payments for date range: %s to %s", startDate, endDate);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(
                () -> this.paymentClient.getPaymentsByDateRange(startDate, endDate, authHeader),
                this.authService, LOG, "fetching payments by date range");
    }

    /** Returns the payments by source account. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public List<PaymentResponseDto> getPaymentsBySourceAccount(final Long sourceId) {
        LOG.debugf("Fetching payments for source account: %s", sourceId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(
                () -> this.paymentClient.getPaymentsBySourceAccount(sourceId, authHeader),
                this.authService, LOG, "fetching payments for source account " + sourceId);
    }

    /** Returns the payments by target account. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public List<PaymentResponseDto> getPaymentsByTargetAccount(final Long targetId) {
        LOG.debugf("Fetching payments for target account: %s", targetId);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(
                () -> this.paymentClient.getPaymentsByTargetAccount(targetId, authHeader),
                this.authService, LOG, "fetching payments for target account " + targetId);
    }

    /** Creates a new payment. */
    public PaymentDto createPayment(final PaymentDto payment) {
        LOG.debugf("Creating new payment: %s", payment);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.paymentClient.createPayment(payment, authHeader),
                this.authService, LOG, "creating payment", "Failed to create payment", response -> {
                    if (response.getStatus() == 201) {
                        final PaymentResponseDto responseDto = ServiceHelper.readEntity(response,
                                PaymentResponseDto.class, "created payment response");
                        return responseDto.toPaymentDto();
                    }
                    throw ServiceHelper.responseFailure("Failed to create payment", response);
                });
    }

    /** Updates the payment. */
    public PaymentDto updatePayment(final PaymentDto payment) {
        LOG.debugf("Updating payment: %s", payment);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.paymentClient.updatePayment(payment.getId(), payment,
                authHeader), this.authService, LOG, "updating payment " + payment.getId(), "Failed to update payment",
                response -> {
                    if (response.getStatus() == 200) {
                        final PaymentResponseDto responseDto = ServiceHelper.readEntity(response,
                                PaymentResponseDto.class, "updated payment response");
                        return responseDto.toPaymentDto();
                    }
                    throw ServiceHelper.responseFailure("Failed to update payment", response);
                });
    }

    /** Handles the delete payment operation. */
    public boolean deletePayment(final Long id) {
        LOG.debugf("Deleting payment with ID: %s", id);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.executeWithResponse(() -> this.paymentClient.deletePayment(id, authHeader),
                this.authService, LOG, "deleting payment " + id, response -> response.getStatus() == 204);
    }

    /** Handles the search payments operation. */
    @Retry(maxRetries = UiConstants.MAX_RETRIES, delay = UiConstants.RETRY_DELAY_MS, delayUnit = ChronoUnit.MILLIS, abortOn = AuthenticationException.class)
    public List<PaymentResponseDto> searchPayments(final String query) {
        LOG.debugf("Searching payments with query: %s", query);
        final var authHeader = this.authService.requireAuthorizationHeader();
        return ServiceHelper.execute(
                () -> this.paymentClient.searchPayments(query, authHeader),
                this.authService, LOG, "searching payments");
    }
}
