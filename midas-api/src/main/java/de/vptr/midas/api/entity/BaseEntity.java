package de.vptr.midas.api.entity;

import java.util.Objects;

import de.vptr.midas.api.util.UlidUtil;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

/** Base class for all entities with a public ULID identifier. */
@MappedSuperclass
public abstract class BaseEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Nullable
    public Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 26, updatable = false)
    @Nullable
    public String publicId;

    /** Automatically generates a public ID before persisting. */
    @PrePersist
    public void generatePublicId() {
        if (this.publicId == null) {
            this.publicId = UlidUtil.generate();
        }
    }

    @Override
    public boolean equals(final Object o) {
        return this == o || (o instanceof final BaseEntity that && Objects.equals(this.id, that.id));
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }
}
