package de.vptr.midas.api.entity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.annotation.Nullable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/** JPA entity representing a user account. */
@Entity
@Table(name = "user_accounts")
public class AccountEntity extends BaseEntity {

    @NotBlank
    @Column(unique = true)
    @Nullable
    public String name;

    @Generated(event = EventType.INSERT)
    @Nullable
    public LocalDateTime created;

    @Generated(event = EventType.UPDATE)
    @Column(name = "last_edit")
    @Nullable
    public LocalDateTime lastEdit;

    @OneToMany(mappedBy = "account", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @Nullable
    public List<UserAccountMetaEntity> userAccountMetas;

    @JsonIgnore
    @OneToMany(mappedBy = "sourceAccount", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @Nullable
    public List<PaymentEntity> outgoingPayments;

    @JsonIgnore
    @OneToMany(mappedBy = "targetAccount", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @Nullable
    public List<PaymentEntity> incomingPayments;

    // Helper method to find account by name
    public static AccountEntity findByName(final String name) {
        return find("name", name).firstResult();
    }

    // Helper method to get users associated with this account
    /** Returns all users associated with this account. */
    public List<UserEntity> getAssociatedUsers() {
        if (this.userAccountMetas == null) {
            return Collections.emptyList();
        }
        return this.userAccountMetas.stream().map(meta -> meta.user).toList();
    }
}
