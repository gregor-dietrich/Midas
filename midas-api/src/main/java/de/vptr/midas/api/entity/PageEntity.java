package de.vptr.midas.api.entity;

import java.time.LocalDateTime;
import java.util.List;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
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
    public Long id;

    @NotBlank
    public String title;

    @Column(columnDefinition = "TEXT")
    @NotBlank
    public String content;

    public LocalDateTime created;

    @Column(name = "last_edit")
    public LocalDateTime lastEdit;

    // Helper method to search pages
    public static List<PageEntity> searchPages(final String searchTerm) {
        final var searchPattern = "%" + searchTerm.toLowerCase() + "%";
        return find("LOWER(title) LIKE ?1 OR LOWER(content) LIKE ?1", searchPattern).list();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final PageEntity that = (PageEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
