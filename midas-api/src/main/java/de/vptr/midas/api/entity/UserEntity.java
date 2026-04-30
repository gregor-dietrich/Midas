package de.vptr.midas.api.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** JPA entity representing a user. */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_rank_id", columnList = "rank_id")
})
public class UserEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    @Column(unique = true)
    public String username;

    @NotBlank
    public String password;

    @NotBlank
    public String salt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rank_id", nullable = false)
    @JsonIgnoreProperties({ "users" })
    public UserRankEntity rank;

    @Email
    @Column(unique = true)
    public String email;

    @Column(nullable = false)
    public boolean banned;

    @Column(nullable = false)
    public boolean activated;

    @Column(name = "activation_key")
    public String activationKey;

    @Column(name = "last_ip", length = 45)
    public String lastIp;

    @Generated(event = EventType.INSERT)
    public LocalDateTime created;

    @Generated(event = EventType.INSERT)
    @Column(name = "last_login")
    public LocalDateTime lastLogin;

    @Generated(event = EventType.UPDATE)
    @Column(name = "last_edit")
    public LocalDateTime lastEdit;

    @OneToMany(mappedBy = "user", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JsonIgnore
    public List<PostEntity> posts;

    @OneToMany(mappedBy = "user", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JsonIgnore
    public List<PostCommentEntity> comments;

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final UserEntity that = (UserEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
