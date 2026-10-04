package de.vptr.midas.api.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import de.vptr.midas.api.security.EncryptedStringConverter;
import jakarta.annotation.Nullable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** JPA entity representing a user. */
@Entity
@Table(name = "users")
public class UserEntity extends BaseEntity {

    @NotBlank
    @Column(unique = true)
    @Nullable
    public String username;

    @NotBlank
    @Nullable
    public String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rank_id", nullable = false)
    @JsonIgnoreProperties({ "users" })
    @Nullable
    public UserRankEntity rank;

    @Email
    @Column(length = 500)
    @Convert(converter = EncryptedStringConverter.class)
    @Nullable
    public String email;

    @Column(name = "email_blind_index", unique = true)
    @Nullable
    public String emailBlindIndex;

    @Column(nullable = false)
    public boolean banned;

    @Column(nullable = false)
    public boolean activated;

    @Column(name = "activation_key")
    @Nullable
    public String activationKey;

    @Column(name = "last_ip", length = 45)
    @Nullable
    public String lastIp;

    @Generated(event = EventType.INSERT)
    @Nullable
    public LocalDateTime created;

    @Generated(event = EventType.INSERT)
    @Column(name = "last_login")
    @Nullable
    public LocalDateTime lastLogin;

    @Generated(event = EventType.UPDATE)
    @Column(name = "last_edit")
    @Nullable
    public LocalDateTime lastEdit;

    @OneToMany(mappedBy = "user", cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE },
            orphanRemoval = true)
    @JsonIgnore
    @Nullable
    public List<PostEntity> posts;

    @OneToMany(mappedBy = "user", cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE },
            orphanRemoval = true)
    @JsonIgnore
    @Nullable
    public List<PostCommentEntity> comments;
}
