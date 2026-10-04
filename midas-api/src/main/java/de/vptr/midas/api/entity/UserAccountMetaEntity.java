package de.vptr.midas.api.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** JPA entity representing the association between a user and an account. */
@Entity
@Table(name = "user_accounts_meta")
public class UserAccountMetaEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @Nullable
    public UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    @Nullable
    public AccountEntity account;

    @Generated(event = EventType.INSERT)
    @Nullable
    public LocalDateTime created;

    @Generated(event = EventType.UPDATE)
    @Column(name = "last_edit")
    @Nullable
    public LocalDateTime lastEdit;

    @Column(name = "is_negative", nullable = false)
    public boolean isNegative = false;

    @Column(name = "can_manage", nullable = false)
    public boolean canManage = false;

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
}
