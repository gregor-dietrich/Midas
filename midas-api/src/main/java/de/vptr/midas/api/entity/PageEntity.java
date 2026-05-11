package de.vptr.midas.api.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/** JPA entity representing a static page. */
@Entity
@Table(name = "pages")
public class PageEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Nullable
    public Long id;

    @NotBlank
    @Nullable
    public String title;

    @Column(columnDefinition = "TEXT")
    @NotBlank
    @Nullable
    public String content;

    @Generated(event = EventType.INSERT)
    @Nullable
    public LocalDateTime created;

    @Generated(event = EventType.UPDATE)
    @Column(name = "last_edit")
    @Nullable
    public LocalDateTime lastEdit;

    // Helper method to search pages
    public static List<PageEntity> searchPages(final String searchTerm) {
        final var searchPattern = "%" + searchTerm.toLowerCase(Locale.ROOT) + "%";
        return find("LOWER(title) LIKE ?1 OR LOWER(content) LIKE ?1", searchPattern).list();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final PageEntity that = (PageEntity) o;
        return this.id != null && this.id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.id);
    }
}
