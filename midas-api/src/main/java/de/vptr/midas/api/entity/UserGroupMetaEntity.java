package de.vptr.midas.api.entity;

import java.time.LocalDateTime;
import java.util.List;

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

/** JPA entity representing the association between a user and a group. */
@Entity
@Table(name = "user_groups_meta", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "group_id" })
}, indexes = {
        @Index(name = "idx_user_group_meta_user_id", columnList = "user_id"),
        @Index(name = "idx_user_group_meta_group_id", columnList = "group_id")
})
public class UserGroupMetaEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    public UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    public UserGroupEntity group;

    @Generated(event = EventType.INSERT)
    public LocalDateTime created;

    @Generated(event = EventType.UPDATE)
    @Column(name = "last_edit")
    public LocalDateTime lastEdit;

    // Helper method to find by user and group
    public static UserGroupMetaEntity findByUserAndGroup(final Long userId, final Long groupId) {
        return find("user.id = ?1 AND group.id = ?2", userId, groupId).firstResult();
    }

    // Helper method to check if user is in group
    public static boolean isUserInGroup(final Long userId, final Long groupId) {
        return count("user.id = ?1 AND group.id = ?2", userId, groupId) > 0;
    }

    // Helper method to find all groups for a user
    public static List<UserGroupMetaEntity> findByUserId(final Long userId) {
        return find("user.id", userId).list();
    }

    // Helper method to find all users in a group
    public static List<UserGroupMetaEntity> findByGroupId(final Long groupId) {
        return find("group.id", groupId).list();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final UserGroupMetaEntity that = (UserGroupMetaEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
