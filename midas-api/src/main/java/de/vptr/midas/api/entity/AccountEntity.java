package de.vptr.midas.api.entity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/** JPA entity representing a user account. */
@Entity
@Table(name = "user_accounts")
public class AccountEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    @Column(unique = true)
    public String name;

    @Generated(event = EventType.INSERT)
    public LocalDateTime created;

    @Generated(event = EventType.UPDATE)
    @Column(name = "last_edit")
    public LocalDateTime lastEdit;

    @OneToMany(mappedBy = "account", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    public List<UserAccountMetaEntity> userAccountMetas;

    @JsonIgnore
    @OneToMany(mappedBy = "sourceAccount", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    public List<PaymentEntity> outgoingPayments;

    @JsonIgnore
    @OneToMany(mappedBy = "targetAccount", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
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
        return this.userAccountMetas.stream()
                .map(meta -> meta.user)
                .toList();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final AccountEntity that = (AccountEntity) o;
        return this.id != null && this.id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hashCode(this.id);
    }
}
