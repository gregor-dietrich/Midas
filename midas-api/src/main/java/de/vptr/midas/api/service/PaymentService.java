package de.vptr.midas.api.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import de.vptr.midas.api.dto.PaymentDto;
import de.vptr.midas.api.dto.PaymentResponseDto;
import de.vptr.midas.api.entity.AccountEntity;
import de.vptr.midas.api.entity.PaymentEntity;
import de.vptr.midas.api.entity.UserEntity;
import de.vptr.midas.api.util.SearchQueryUtil;
import jakarta.annotation.Nullable;
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
    private static final String PAYMENT_ACCOUNT_REQUIRED_MESSAGE =
            "At least one account (source or target) must be specified";

    @Inject
    AccountAccessService accountAccessService;

    @Inject
    AdminAccessService adminAccessService;

    private void verifyPaymentAccess(@Nullable final String currentUsername, final PaymentEntity payment) {
        if (currentUsername == null || currentUsername.isBlank()) {
            throw new WebApplicationException(PAYMENT_ACCESS_DENIED_MESSAGE, Response.Status.FORBIDDEN);
        }
        if (!payment.hasLinkedAccount()) {
            throw new WebApplicationException(PAYMENT_ACCESS_DENIED_MESSAGE, Response.Status.FORBIDDEN);
        }
        boolean hasAccess = false;
        if (payment.sourceAccount != null && payment.sourceAccount.publicId != null
                && this.accountAccessService.hasUserAccessToAccount(currentUsername, payment.sourceAccount.publicId)) {
            hasAccess = true;
        }
        if (!hasAccess && payment.targetAccount != null && payment.targetAccount.publicId != null
                && this.accountAccessService.hasUserAccessToAccount(currentUsername, payment.targetAccount.publicId)) {
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
        if (paymentDto.targetAccountPublicId == null && paymentDto.sourceAccountPublicId == null) {
            throw new ValidationException(PAYMENT_ACCOUNT_REQUIRED_MESSAGE);
        }
    }

    @Nullable
    private static AccountEntity resolveAccount(@Nullable final String accountPublicId, final String accountType) {
        if (accountPublicId == null) {
            return null;
        }
        final AccountEntity account = AccountEntity.find("publicId", accountPublicId).firstResult();
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
        return PaymentEntity.find(
                "SELECT p FROM PaymentEntity p LEFT JOIN FETCH p.targetAccount LEFT JOIN FETCH p.sourceAccount LEFT JOIN FETCH p.user ORDER BY p.date DESC")
                .page(page, size).list().stream().map(entity -> new PaymentResponseDto((PaymentEntity) entity))
                .toList();
    }

    /** Returns the payment with the given publicId. */
    public Optional<PaymentResponseDto> findById(final String publicId, @Nullable final String currentUsername) {
        final PaymentEntity entity = PaymentEntity.find(
                "SELECT p FROM PaymentEntity p LEFT JOIN FETCH p.targetAccount LEFT JOIN FETCH p.sourceAccount LEFT JOIN FETCH p.user WHERE p.publicId = ?1",
                publicId).firstResult();
        if (entity == null) {
            return Optional.empty();
        }
        this.verifyPaymentAccess(currentUsername, entity);
        return Optional.of(new PaymentResponseDto(entity));
    }

    /** Returns all payments for the given user publicId. */
    public List<PaymentResponseDto> findByUserPublicId(final String userPublicId) {
        return PaymentEntity.find(
                "SELECT p FROM PaymentEntity p LEFT JOIN FETCH p.targetAccount LEFT JOIN FETCH p.sourceAccount LEFT JOIN FETCH p.user WHERE p.user.publicId = ?1",
                userPublicId).list().stream().map(entity -> new PaymentResponseDto((PaymentEntity) entity)).toList();
    }

    /** Returns all payments for the given source account publicId. */
    public List<PaymentResponseDto> findBySourceAccountPublicId(final String sourcePublicId) {
        return PaymentEntity.find(
                "SELECT p FROM PaymentEntity p LEFT JOIN FETCH p.targetAccount LEFT JOIN FETCH p.sourceAccount LEFT JOIN FETCH p.user WHERE p.sourceAccount.publicId = ?1",
                sourcePublicId).list().stream().map(entity -> new PaymentResponseDto((PaymentEntity) entity)).toList();
    }

    /** Returns all payments for the given target account publicId. */
    public List<PaymentResponseDto> findByTargetAccountPublicId(final String targetPublicId) {
        return PaymentEntity.find(
                "SELECT p FROM PaymentEntity p LEFT JOIN FETCH p.targetAccount LEFT JOIN FETCH p.sourceAccount LEFT JOIN FETCH p.user WHERE p.targetAccount.publicId = ?1",
                targetPublicId).list().stream().map(entity -> new PaymentResponseDto((PaymentEntity) entity)).toList();
    }

    /** Returns all payments within the given date range. */
    public List<PaymentResponseDto> findByDateRange(final LocalDate startDate, final LocalDate endDate) {
        return PaymentEntity.find(
                "SELECT p FROM PaymentEntity p LEFT JOIN FETCH p.targetAccount LEFT JOIN FETCH p.sourceAccount LEFT JOIN FETCH p.user WHERE p.date BETWEEN ?1 AND ?2 ORDER BY p.date DESC",
                startDate, endDate).list().stream().map(entity -> new PaymentResponseDto((PaymentEntity) entity))
                .toList();
    }

    /** Returns the most recent payments up to the given limit. */
    public List<PaymentResponseDto> findRecentPayments(final int limit) {
        return PaymentEntity.find(
                "SELECT p FROM PaymentEntity p LEFT JOIN FETCH p.targetAccount LEFT JOIN FETCH p.sourceAccount LEFT JOIN FETCH p.user ORDER BY p.date DESC")
                .page(0, limit).list().stream().map(entity -> new PaymentResponseDto((PaymentEntity) entity)).toList();
    }

    /** Returns the total payment amount for the given user publicId. */
    public BigDecimal getTotalAmountByUserPublicId(final String userPublicId) {
        final UserEntity user = UserEntity.find("publicId", userPublicId).firstResult();
        if (user == null || user.id == null) {
            return BigDecimal.ZERO;
        }
        return Optional.ofNullable(PaymentEntity.getTotalAmountByUser(user.id)).orElse(BigDecimal.ZERO);
    }

    /** Creates a new payment. */
    @Transactional
    public PaymentResponseDto createPayment(final PaymentDto paymentDto) {
        validatePaymentAccountIds(paymentDto);

        final AccountEntity targetAccount = resolveAccount(paymentDto.targetAccountPublicId, "Target");
        final AccountEntity sourceAccount = resolveAccount(paymentDto.sourceAccountPublicId, "Source");

        final UserEntity user = UserEntity.find("publicId", paymentDto.userPublicId).firstResult();
        if (user == null) {
            throw new ValidationException("User not found");
        }

        final PaymentEntity payment = new PaymentEntity();
        payment.targetAccount = targetAccount;
        payment.sourceAccount = sourceAccount;
        payment.user = user;
        payment.comment = paymentDto.comment;
        payment.date = paymentDto.date;
        payment.amount = paymentDto.amount;

        validatePaymentAccounts(payment);
        payment.persistAndFlush();
        return new PaymentResponseDto(payment);
    }

    /** Updates the payment with the given publicId. */
    @Transactional
    public PaymentResponseDto updatePayment(final String publicId, final PaymentDto paymentDto,
            @Nullable final String currentUsername) {
        validatePaymentAccountIds(paymentDto);

        final PaymentEntity existingPayment = PaymentEntity.find("publicId", publicId).firstResult();
        if (existingPayment == null) {
            throw new WebApplicationException("Payment not found", Response.Status.NOT_FOUND);
        }

        this.verifyPaymentAccess(currentUsername, existingPayment);

        final AccountEntity targetAccount = resolveAccount(paymentDto.targetAccountPublicId, "Target");
        final AccountEntity sourceAccount = resolveAccount(paymentDto.sourceAccountPublicId, "Source");

        final UserEntity user = UserEntity.find("publicId", paymentDto.userPublicId).firstResult();
        if (user == null) {
            throw new ValidationException("User not found");
        }

        // Complete replacement (PUT semantics)
        existingPayment.targetAccount = targetAccount;
        existingPayment.sourceAccount = sourceAccount;
        existingPayment.user = user;
        existingPayment.comment = paymentDto.comment;
        existingPayment.date = paymentDto.date;
        existingPayment.amount = paymentDto.amount;

        validatePaymentAccounts(existingPayment);
        existingPayment.persistAndFlush();
        return new PaymentResponseDto(existingPayment);
    }

    /**
     * Creates a payment scoped to the given user and account. Validates that the user has access to all accounts
     * involved.
     */
    @Transactional
    public PaymentResponseDto createPaymentForUser(final PaymentDto paymentDto, final String username,
            final String accountPublicId) {
        if (!this.accountAccessService.canUserManage(username, accountPublicId)) {
            throw new WebApplicationException("Read-only access for this account", Response.Status.FORBIDDEN);
        }
        this.verifyAccountAccessForPayment(username, accountPublicId, paymentDto);
        final UserEntity currentUser = UserEntity.find("username", username).firstResult();
        paymentDto.userPublicId = currentUser != null ? currentUser.publicId : null;
        return this.createPayment(paymentDto);
    }

    /**
     * Updates a payment scoped to the given user and account. Validates that the user has access to the account and
     * that the payment belongs to it.
     */
    @Transactional
    public PaymentResponseDto updatePaymentForUser(final String paymentPublicId, final PaymentDto paymentDto,
            final String username, final String accountPublicId) {
        final PaymentEntity existingPayment = this.verifyPaymentWriteAccess(paymentPublicId, username, accountPublicId);

        this.verifyCounterpartyAccess(existingPayment, username, accountPublicId,
                "Cannot modify payment with inaccessible counterparty");

        if (existingPayment.user != null) {
            paymentDto.userPublicId = existingPayment.user.publicId;
        } else {
            final UserEntity currentUser = UserEntity.find("username", username).firstResult();
            paymentDto.userPublicId = currentUser != null ? currentUser.publicId : null;
        }
        if (paymentDto.sourceAccountPublicId != null
                && !this.accountAccessService.hasUserAccessToAccount(username, paymentDto.sourceAccountPublicId)) {
            throw new WebApplicationException("Access denied to source account", Response.Status.FORBIDDEN);
        }
        if (paymentDto.targetAccountPublicId != null
                && !this.accountAccessService.hasUserAccessToAccount(username, paymentDto.targetAccountPublicId)) {
            throw new WebApplicationException("Access denied to target account", Response.Status.FORBIDDEN);
        }
        return this.updatePayment(paymentPublicId, paymentDto, username);
    }

    /**
     * Deletes a payment scoped to the given user and account. Validates that the user has access to the account, can
     * manage it, and that the payment belongs to it.
     */
    @Transactional
    public boolean deletePaymentForUser(final String paymentPublicId, final String username,
            final String accountPublicId) {
        final PaymentEntity existingPayment = this.verifyPaymentWriteAccess(paymentPublicId, username, accountPublicId);

        this.verifyCounterpartyAccess(existingPayment, username, accountPublicId,
                "Cannot delete payment with inaccessible counterparty");

        return this.deletePayment(paymentPublicId, username);
    }

    private PaymentEntity verifyPaymentWriteAccess(final String paymentPublicId, final String username,
            final String accountPublicId) {
        if (!this.accountAccessService.hasUserAccessToAccount(username, accountPublicId)) {
            throw new WebApplicationException("Access denied to account", Response.Status.FORBIDDEN);
        }
        if (!this.accountAccessService.canUserManage(username, accountPublicId)) {
            throw new WebApplicationException("Read-only access for this account", Response.Status.FORBIDDEN);
        }

        final PaymentEntity existingPayment = PaymentEntity.find("publicId", paymentPublicId).firstResult();
        if (existingPayment == null) {
            throw new WebApplicationException("Payment not found", Response.Status.NOT_FOUND);
        }
        if (!paymentBelongsToAccount(existingPayment, accountPublicId)) {
            throw new WebApplicationException("Payment does not belong to this account", Response.Status.FORBIDDEN);
        }
        return existingPayment;
    }

    private void verifyCounterpartyAccess(final PaymentEntity existingPayment, final String username,
            final String accountPublicId, final String errorMessage) {
        // Counterparty rule: if payment has source AND target, user must be associated with both
        if (existingPayment.sourceAccount != null && existingPayment.targetAccount != null) {
            final String sourceId = Objects.requireNonNull(existingPayment.sourceAccount.publicId);
            final String targetId = Objects.requireNonNull(existingPayment.targetAccount.publicId);
            final String counterpartyId = accountPublicId.equals(sourceId) ? targetId : sourceId;
            if (!this.accountAccessService.hasUserAccessToAccount(username, counterpartyId)) {
                throw new WebApplicationException(errorMessage, Response.Status.FORBIDDEN);
            }
        }
    }

    private void verifyAccountAccessForPayment(final String username, final String accountPublicId,
            final PaymentDto paymentDto) {
        if (paymentDto.sourceAccountPublicId != null && paymentDto.sourceAccountPublicId.equals(accountPublicId)
                && !this.accountAccessService.hasUserAccessToAccount(username, accountPublicId)) {
            throw new WebApplicationException("Access denied to source account", Response.Status.FORBIDDEN);
        }
        if (paymentDto.targetAccountPublicId != null && paymentDto.targetAccountPublicId.equals(accountPublicId)
                && !this.accountAccessService.hasUserAccessToAccount(username, accountPublicId)) {
            throw new WebApplicationException("Access denied to target account", Response.Status.FORBIDDEN);
        }
        if (paymentDto.sourceAccountPublicId != null && !paymentDto.sourceAccountPublicId.equals(accountPublicId)
                && !this.accountAccessService.hasUserAccessToAccount(username, paymentDto.sourceAccountPublicId)) {
            throw new WebApplicationException("Access denied to source account", Response.Status.FORBIDDEN);
        }
        if (paymentDto.targetAccountPublicId != null && !paymentDto.targetAccountPublicId.equals(accountPublicId)
                && !this.accountAccessService.hasUserAccessToAccount(username, paymentDto.targetAccountPublicId)) {
            throw new WebApplicationException("Access denied to target account", Response.Status.FORBIDDEN);
        }
    }

    private static boolean paymentBelongsToAccount(final PaymentEntity payment, final String accountPublicId) {
        return (payment.sourceAccount != null && accountPublicId.equals(payment.sourceAccount.publicId))
                || (payment.targetAccount != null && accountPublicId.equals(payment.targetAccount.publicId));
    }

    /** Partially updates the payment with the given publicId. */
    @Transactional
    public PaymentResponseDto patchPayment(final String publicId, final PaymentDto paymentDto,
            @Nullable final String currentUsername) {
        final PaymentEntity existingPayment = PaymentEntity.find("publicId", publicId).firstResult();
        if (existingPayment == null) {
            throw new WebApplicationException("Payment not found", Response.Status.NOT_FOUND);
        }

        this.verifyPaymentAccess(currentUsername, existingPayment);

        // Partial update (PATCH semantics) - only update provided fields
        if (paymentDto.targetAccountPublicId != null) {
            existingPayment.targetAccount = resolveAccount(paymentDto.targetAccountPublicId, "Target");
        }
        if (paymentDto.sourceAccountPublicId != null) {
            existingPayment.sourceAccount = resolveAccount(paymentDto.sourceAccountPublicId, "Source");
        }
        if (paymentDto.userPublicId != null) {
            // Block re-attribution of payments to a different user
            final String existingUserPublicId = existingPayment.user != null ? existingPayment.user.publicId : null;
            if (!paymentDto.userPublicId.equals(existingUserPublicId)) {
                throw new ValidationException("Payment user re-attribution is not allowed.");
            }
        }
        if (paymentDto.comment != null) {
            if (paymentDto.comment.trim().isEmpty()) {
                throw new ValidationException("Comment cannot be blank");
            }
            existingPayment.comment = paymentDto.comment;
        }
        if (paymentDto.date != null) {
            existingPayment.date = paymentDto.date;
        }
        if (paymentDto.amount != null) {
            if (paymentDto.amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Amount must be positive");
            }
            existingPayment.amount = paymentDto.amount;
        }

        // Validate that at least one account is still present after the patch
        validatePaymentAccounts(existingPayment);
        existingPayment.persistAndFlush();
        return new PaymentResponseDto(existingPayment);
    }

    /** Deletes the payment with the given publicId. */
    @Transactional
    public boolean deletePayment(final String publicId, @Nullable final String currentUsername) {
        final PaymentEntity existingPayment = PaymentEntity.find("publicId", publicId).firstResult();
        if (existingPayment == null) {
            return false;
        }
        this.verifyPaymentAccess(currentUsername, existingPayment);
        return PaymentEntity.delete("publicId", publicId) > 0;
    }

    /** Returns all payments matching the given query. */
    public List<PaymentResponseDto> searchPayments(@Nullable final String query) {
        final String searchTerm = SearchQueryUtil.toContainsLikePattern(query);
        if (searchTerm == null) {
            return this.getAllPayments();
        }

        final List<PaymentEntity> payments = SearchQueryUtil.applyDefaultSearchLimit(PaymentEntity.find("""
                SELECT p FROM PaymentEntity p \
                LEFT JOIN FETCH p.targetAccount LEFT JOIN FETCH p.sourceAccount LEFT JOIN FETCH p.user \
                WHERE LOWER(p.comment) LIKE ?1 ESCAPE '\\' \
                OR LOWER(p.sourceAccount.name) LIKE ?1 ESCAPE '\\' \
                OR LOWER(p.targetAccount.name) LIKE ?1 ESCAPE '\\' \
                ORDER BY p.date DESC""", searchTerm)).list();

        return payments.stream().map(PaymentResponseDto::new).toList();
    }
}
