package de.vptr.midas.api.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** JPA entity representing a payment. */
@Entity
@Table(name = "user_payments",
        indexes = { @Index(name = "idx_payment_target_id", columnList = "target_id"),
                @Index(name = "idx_payment_source_id", columnList = "source_id"),
                @Index(name = "idx_payment_user_id", columnList = "user_id") })
public class PaymentEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Nullable
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id")
    @Nullable
    public AccountEntity targetAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    @Nullable
    public AccountEntity sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @Nullable
    public UserEntity user;

    @NotBlank
    @Nullable
    public String comment;

    @SuppressWarnings("MultipleNullnessAnnotations")
    @NotNull
    @Nullable
    public LocalDate date;

    @SuppressWarnings("MultipleNullnessAnnotations")
    @NotNull
    @DecimalMin(value = "0.00", message = "Amount must be positive")
    @Column(precision = 15, scale = 2)
    @Nullable
    public BigDecimal amount;

    @Generated(event = EventType.INSERT)
    @Nullable
    public LocalDateTime created;

    @Generated(event = EventType.UPDATE)
    @Column(name = "last_edit")
    @Nullable
    public LocalDateTime lastEdit;

    /** Returns true when payment is linked to at least one account. */
    public boolean hasLinkedAccount() {
        return this.targetAccount != null || this.sourceAccount != null;
    }

    // Helper methods for queries
    public static List<PaymentEntity> findByUserId(final Long userId) {
        return find("user.id", userId).list();
    }

    public static List<PaymentEntity> findBySourceAccountId(final Long sourceId) {
        return find("sourceAccount.id", sourceId).list();
    }

    public static List<PaymentEntity> findByTargetAccountId(final Long targetId) {
        return find("targetAccount.id", targetId).list();
    }

    public static List<PaymentEntity> findByDateRange(final LocalDate startDate, final LocalDate endDate) {
        return find("date BETWEEN ?1 AND ?2 ORDER BY date DESC", startDate, endDate).list();
    }

    public static List<PaymentEntity> findRecentPayments(final int limit) {
        return find("ORDER BY date DESC").page(0, limit).list();
    }

    // Helper method to calculate total amount for a user
    /** Returns the total payment amount for the given user. */
    public static BigDecimal getTotalAmountByUser(final Long userId) {
        return getEntityManager()
                .createQuery("SELECT SUM(p.amount) FROM PaymentEntity p WHERE p.user.id = :userId", BigDecimal.class)
                .setParameter("userId", userId).getSingleResult();
    }

    @Override
    public boolean equals(final Object o) {
        return this == o || (o instanceof PaymentEntity that && Objects.equals(this.id, that.id));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }
}
