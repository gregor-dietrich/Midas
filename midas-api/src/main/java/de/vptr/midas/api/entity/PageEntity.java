package de.vptr.midas.api.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/** JPA entity representing a static page. */
@Entity
@Table(name = "pages")
public class PageEntity extends BaseEntity {

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
}
