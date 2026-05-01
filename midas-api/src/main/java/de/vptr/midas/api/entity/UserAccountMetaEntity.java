package de.vptr.midas.api.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
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
import jakarta.persistence.UniqueConstraint;

/** JPA entity representing the association between a user and an account. */
@Entity
@Table(name = "user_accounts_meta", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "account_id" })
}, indexes = {
        @Index(name = "idx_user_account_meta_user_id", columnList = "user_id"),
        @Index(name = "idx_user_account_meta_account_id", columnList = "account_id")
})
public class UserAccountMetaEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    public AccountEntity account;

    @Generated(event = EventType.INSERT)
    public LocalDateTime created;

    @Generated(event = EventType.UPDATE)
    @Column(name = "last_edit")
    public LocalDateTime lastEdit;

    @Column(name = "is_negative", nullable = false)
    public boolean isNegative = false;

    // Helper method to find by user and account
    public static UserAccountMetaEntity findByUserAndAccount(final Long userId, final Long accountId) {
        return find("user.id = ?1 AND account.id = ?2", userId, accountId).firstResult();
    }

    // Helper method to check if association exists
    public static boolean existsByUserAndAccount(final Long userId, final Long accountId) {
        return count("user.id = ?1 AND account.id = ?2", userId, accountId) > 0;
    }

    // Helper method to find all accounts for a user
    public static List<UserAccountMetaEntity> findByUserId(final Long userId) {
        return find("user.id", userId).list();
    }

    // Helper method to find all users for an account
    public static List<UserAccountMetaEntity> findByAccountId(final Long accountId) {
        return find("account.id", accountId).list();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final UserAccountMetaEntity that = (UserAccountMetaEntity) o;
        return this.id != null && this.id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }
}
