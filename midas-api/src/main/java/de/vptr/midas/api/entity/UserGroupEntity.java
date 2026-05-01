package de.vptr.midas.api.entity;

import java.time.LocalDateTime;
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

/** JPA entity representing a user group. */
@Entity
@Table(name = "user_groups")
public class UserGroupEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    public String name;

    @Generated(event = EventType.INSERT)
    public LocalDateTime created;

    @Generated(event = EventType.UPDATE)
    @Column(name = "last_edit")
    public LocalDateTime lastEdit;

    @JsonIgnore
    @OneToMany(mappedBy = "group", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    public List<UserGroupMetaEntity> userGroupMetas;

    // Helper method to find group by name
    public static UserGroupEntity findByName(final String name) {
        return find("name", name).firstResult();
    }

    // Helper method to get users in this group
    /** Returns all users in this group. */
    public List<UserEntity> getUsers() {
        if (this.userGroupMetas == null) {
            return java.util.List.of();
        }
        return this.userGroupMetas.stream()
                .map(meta -> meta.user)
                .toList();
    }

    // Helper method to get user count in this group
    public long getUserCount() {
        return this.userGroupMetas != null ? this.userGroupMetas.size() : 0;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final UserGroupEntity that = (UserGroupEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hashCode(this.id);
    }
}
