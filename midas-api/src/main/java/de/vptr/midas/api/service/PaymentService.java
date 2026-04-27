package de.vptr.midas.api.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import de.vptr.midas.api.dto.PaymentDto;
import de.vptr.midas.api.dto.PaymentResponseDto;
import de.vptr.midas.api.entity.AccountEntity;
import de.vptr.midas.api.entity.PaymentEntity;
import de.vptr.midas.api.entity.UserEntity;
import de.vptr.midas.api.util.SearchQueryUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/** Service for payment business logic. */
@ApplicationScoped
public class PaymentService {

    private static final String PAYMENT_ACCESS_DENIED_MESSAGE = "Access denied to payment";
    private static final String PAYMENT_ACCOUNT_REQUIRED_MESSAGE = "At least one account (source or target) must be specified";

    @Inject
    AccountAccessService accountAccessService;

    @Inject
    AdminAccessService adminAccessService;

    private void verifyPaymentAccess(final String currentUsername, final PaymentEntity payment) {
        if (currentUsername == null || currentUsername.isBlank()) {
            throw new WebApplicationException(PAYMENT_ACCESS_DENIED_MESSAGE, Response.Status.FORBIDDEN);
        }
        if (!payment.hasLinkedAccount()) {
            throw new WebApplicationException(PAYMENT_ACCESS_DENIED_MESSAGE, Response.Status.FORBIDDEN);
        }
        boolean hasAccess = false;
        if (payment.sourceAccount != null
                && this.accountAccessService.hasUserAccessToAccount(currentUsername, payment.sourceAccount.id)) {
            hasAccess = true;
        }
        if (!hasAccess && payment.targetAccount != null
                && this.accountAccessService.hasUserAccessToAccount(currentUsername, payment.targetAccount.id)) {
            hasAccess = true;
        }
        if (!hasAccess && !this.adminAccessService.hasAdminViewAccess(currentUsername)) {
            throw new WebApplicationException(PAYMENT_ACCESS_DENIED_MESSAGE, Response.Status.FORBIDDEN);
        }
    }

    private static void validatePaymentAccounts(final PaymentEntity payment) {
        if (!payment.hasLinkedAccount()) {
            throw new ValidationException(PAYMENT_ACCOUNT_REQUIRED_MESSAGE);
        }
    }

    private static void validatePaymentAccountIds(final PaymentDto paymentDto) {
        if (paymentDto.targetAccountId == null && paymentDto.sourceAccountId == null) {
            throw new ValidationException(PAYMENT_ACCOUNT_REQUIRED_MESSAGE);
        }
    }

    private static AccountEntity resolveAccount(final Long accountId, final String accountType) {
        if (accountId == null) {
            return null;
        }
        final AccountEntity account = AccountEntity.findById(accountId);
        if (account == null) {
            throw new ValidationException(accountType + " account not found");
        }
        return account;
    }

    /** Returns all payments with default pagination. */
    public List<PaymentResponseDto> getAllPayments() {
        return this.getAllPayments(0, 20);
    }

    /** Returns all payments with the given pagination. */
    public List<PaymentResponseDto> getAllPayments(final int page, final int size) {
        return PaymentEntity
                .find("SELECT p FROM PaymentEntity p LEFT JOIN FETCH p.targetAccount LEFT JOIN FETCH p.sourceAccount LEFT JOIN FETCH p.userId ORDER BY p.date DESC")
                .page(page, size)
                .list()
                .stream()
                .map(entity -> new PaymentResponseDto((PaymentEntity) entity))
                .toList();
    }

    /** Returns the payment with the given id. */
    public Optional<PaymentResponseDto> findById(final Long id, final String currentUsername) {
        final PaymentEntity entity = PaymentEntity
                .find("SELECT p FROM PaymentEntity p LEFT JOIN FETCH p.targetAccount LEFT JOIN FETCH p.sourceAccount LEFT JOIN FETCH p.userId WHERE p.id = ?1",
                        id)
                .firstResult();
        if (entity == null) {
            return Optional.empty();
        }
        this.verifyPaymentAccess(currentUsername, entity);
        return Optional.of(new PaymentResponseDto(entity));
    }

    /** Returns all payments for the given user. */
    public List<PaymentResponseDto> findByUserId(final Long userId) {
        return PaymentEntity
                .find("SELECT p FROM PaymentEntity p LEFT JOIN FETCH p.targetAccount LEFT JOIN FETCH p.sourceAccount LEFT JOIN FETCH p.userId WHERE p.userId.id = ?1",
                        userId)
                .list().stream()
                .map(entity -> new PaymentResponseDto((PaymentEntity) entity))
                .toList();
    }

    /** Returns all payments for the given source account. */
    public List<PaymentResponseDto> findBySourceAccountId(final Long sourceId) {
        return PaymentEntity
                .find("SELECT p FROM PaymentEntity p LEFT JOIN FETCH p.targetAccount LEFT JOIN FETCH p.sourceAccount LEFT JOIN FETCH p.userId WHERE p.sourceAccount.id = ?1",
                        sourceId)
                .list().stream()
                .map(entity -> new PaymentResponseDto((PaymentEntity) entity))
                .toList();
    }

    /** Returns all payments for the given target account. */
    public List<PaymentResponseDto> findByTargetAccountId(final Long targetId) {
        return PaymentEntity
                .find("SELECT p FROM PaymentEntity p LEFT JOIN FETCH p.targetAccount LEFT JOIN FETCH p.sourceAccount LEFT JOIN FETCH p.userId WHERE p.targetAccount.id = ?1",
                        targetId)
                .list().stream()
                .map(entity -> new PaymentResponseDto((PaymentEntity) entity))
                .toList();
    }

    /** Returns all payments within the given date range. */
    public List<PaymentResponseDto> findByDateRange(final LocalDate startDate, final LocalDate endDate) {
        return PaymentEntity
                .find("SELECT p FROM PaymentEntity p LEFT JOIN FETCH p.targetAccount LEFT JOIN FETCH p.sourceAccount LEFT JOIN FETCH p.userId WHERE p.date BETWEEN ?1 AND ?2 ORDER BY p.date DESC",
                        startDate, endDate)
                .list().stream()
                .map(entity -> new PaymentResponseDto((PaymentEntity) entity))
                .toList();
    }

    /** Returns the most recent payments up to the given limit. */
    public List<PaymentResponseDto> findRecentPayments(final int limit) {
        return PaymentEntity
                .find("SELECT p FROM PaymentEntity p LEFT JOIN FETCH p.targetAccount LEFT JOIN FETCH p.sourceAccount LEFT JOIN FETCH p.userId ORDER BY p.date DESC")
                .page(0, limit).list().stream()
                .map(entity -> new PaymentResponseDto((PaymentEntity) entity))
                .toList();
    }

    /** Returns the total payment amount for the given user. */
    public BigDecimal getTotalAmountByUser(final Long userId) {
        return Optional.ofNullable(PaymentEntity.getTotalAmountByUser(userId)).orElse(BigDecimal.ZERO);
    }

    /** Creates a new payment. */
    @Transactional
    public PaymentResponseDto createPayment(final PaymentDto paymentDto) {
        validatePaymentAccountIds(paymentDto);
        if (paymentDto.userId == null) {
            throw new ValidationException("User ID is required");
        }
        if (paymentDto.comment == null || paymentDto.comment.trim().isEmpty()) {
            throw new ValidationException("Comment is required");
        }
        if (paymentDto.date == null) {
            throw new ValidationException("Date is required");
        }
        if (paymentDto.amount == null || paymentDto.amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Amount must be positive");
        }

        final AccountEntity targetAccount = resolveAccount(paymentDto.targetAccountId, "Target");
        final AccountEntity sourceAccount = resolveAccount(paymentDto.sourceAccountId, "Source");

        final UserEntity user = UserEntity.findById(paymentDto.userId);
        if (user == null) {
            throw new ValidationException("User not found");
        }

        final PaymentEntity payment = new PaymentEntity();
        payment.targetAccount = targetAccount;
        payment.sourceAccount = sourceAccount;
        payment.userId = user;
        payment.comment = paymentDto.comment;
        payment.date = paymentDto.date;
        payment.amount = paymentDto.amount;
        payment.created = LocalDateTime.now();
        payment.lastEdit = payment.created;

        validatePaymentAccounts(payment);
        payment.persist();
        return new PaymentResponseDto(payment);
    }

    /** Updates the payment with the given id. */
    @Transactional
    public PaymentResponseDto updatePayment(final Long id, final PaymentDto paymentDto,
            final String currentUsername) {
        validatePaymentAccountIds(paymentDto);
        if (paymentDto.userId == null) {
            throw new ValidationException("User ID is required");
        }
        if (paymentDto.comment == null || paymentDto.comment.trim().isEmpty()) {
            throw new ValidationException("Comment is required");
        }
        if (paymentDto.date == null) {
            throw new ValidationException("Date is required");
        }
        if (paymentDto.amount == null || paymentDto.amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Amount must be positive");
        }

        final PaymentEntity existingPayment = PaymentEntity.findById(id);
        if (existingPayment == null) {
            throw new WebApplicationException("Payment not found", Response.Status.NOT_FOUND);
        }

        this.verifyPaymentAccess(currentUsername, existingPayment);

        final AccountEntity targetAccount = resolveAccount(paymentDto.targetAccountId, "Target");
        final AccountEntity sourceAccount = resolveAccount(paymentDto.sourceAccountId, "Source");

        final UserEntity user = UserEntity.findById(paymentDto.userId);
        if (user == null) {
            throw new ValidationException("User not found");
        }

        // Complete replacement (PUT semantics)
        existingPayment.targetAccount = targetAccount;
        existingPayment.sourceAccount = sourceAccount;
        existingPayment.userId = user;
        existingPayment.comment = paymentDto.comment;
        existingPayment.date = paymentDto.date;
        existingPayment.amount = paymentDto.amount;
        existingPayment.lastEdit = LocalDateTime.now();

        validatePaymentAccounts(existingPayment);
        existingPayment.persist();
        return new PaymentResponseDto(existingPayment);
    }

    /** Partially updates the payment with the given id. */
    @Transactional
    public PaymentResponseDto patchPayment(final Long id, final PaymentDto paymentDto,
            final String currentUsername) {
        final PaymentEntity existingPayment = PaymentEntity.findById(id);
        if (existingPayment == null) {
            throw new WebApplicationException("Payment not found", Response.Status.NOT_FOUND);
        }

        this.verifyPaymentAccess(currentUsername, existingPayment);

        // Partial update (PATCH semantics) - only update provided fields
        if (paymentDto.targetAccountId != null) {
            existingPayment.targetAccount = resolveAccount(paymentDto.targetAccountId, "Target");
        }
        if (paymentDto.sourceAccountId != null) {
            existingPayment.sourceAccount = resolveAccount(paymentDto.sourceAccountId, "Source");
        }
        if (paymentDto.userId != null) {
            final UserEntity user = UserEntity.findById(paymentDto.userId);
            if (user == null) {
                throw new ValidationException("User not found");
            }
            existingPayment.userId = user;
        }
        if (paymentDto.comment != null && !paymentDto.comment.trim().isEmpty()) {
            existingPayment.comment = paymentDto.comment;
        }
        if (paymentDto.date != null) {
            existingPayment.date = paymentDto.date;
        }
        if (paymentDto.amount != null && paymentDto.amount.compareTo(BigDecimal.ZERO) > 0) {
            existingPayment.amount = paymentDto.amount;
        }

        // Validate that at least one account is still present after the patch
        validatePaymentAccounts(existingPayment);
        existingPayment.lastEdit = LocalDateTime.now();
        existingPayment.persist();
        return new PaymentResponseDto(existingPayment);
    }

    /** Deletes the payment with the given id. */
    @Transactional
    public boolean deletePayment(final Long id, final String currentUsername) {
        final PaymentEntity existingPayment = PaymentEntity.findById(id);
        if (existingPayment == null) {
            return false;
        }
        this.verifyPaymentAccess(currentUsername, existingPayment);
        return PaymentEntity.deleteById(id);
    }

    /** Returns all payments matching the given query. */
    public List<PaymentResponseDto> searchPayments(final String query) {
        final String searchTerm = SearchQueryUtil.toContainsLikePattern(query);
        if (searchTerm == null) {
            return this.getAllPayments();
        }

        final List<PaymentEntity> payments = SearchQueryUtil.applyDefaultSearchLimit(PaymentEntity.find(
                "SELECT p FROM PaymentEntity p LEFT JOIN FETCH p.targetAccount LEFT JOIN FETCH p.sourceAccount LEFT JOIN FETCH p.userId "
                        + "WHERE LOWER(p.comment) LIKE ?1 ESCAPE '\\' "
                        + "OR LOWER(p.sourceAccount.name) LIKE ?1 ESCAPE '\\' "
                        + "OR LOWER(p.targetAccount.name) LIKE ?1 ESCAPE '\\' "
                        + "ORDER BY p.date DESC",
                searchTerm)).list();

        return payments.stream()
                .map(PaymentResponseDto::new)
                .toList();
    }
}
