package de.vptr.midas.api.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

/** JPA entity representing a user group. */
@Entity
@Table(name = "user_groups")
public class UserGroupEntity extends BaseEntity {

    @NotBlank
    @Nullable
    public String name;

    @Generated(event = EventType.INSERT)
    @Nullable
    public LocalDateTime created;

    @Generated(event = EventType.UPDATE)
    @Column(name = "last_edit")
    @Nullable
    public LocalDateTime lastEdit;

    @JsonIgnore
    @OneToMany(mappedBy = "group", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @Nullable
    public List<UserGroupMetaEntity> userGroupMetas;

    // Helper method to find group by name
    public static UserGroupEntity findByName(final String name) {
        return find("name", name).firstResult();
    }

    // Helper method to get users in this group
    /** Returns all users in this group. */
    public List<UserEntity> getUsers() {
        if (this.userGroupMetas == null) {
            return List.of();
        }
        final List<UserEntity> users = new ArrayList<>();
        for (final UserGroupMetaEntity meta : this.userGroupMetas) {
            if (meta.user != null) {
                users.add(meta.user);
            }
        }
        return users;
    }

    // Helper method to get user count in this group
    public long getUserCount() {
        return this.userGroupMetas != null ? this.userGroupMetas.size() : 0;
    }
}
