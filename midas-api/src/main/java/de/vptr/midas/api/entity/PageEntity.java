package de.vptr.midas.api.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import de.vptr.midas.api.util.SearchQueryUtil;
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

    /** Returns pages whose title or content match the given term, with LIKE wildcards escaped and results bounded. */
    public static List<PageEntity> searchPages(final String searchTerm) {
        final String like = SearchQueryUtil.toContainsLikePattern(searchTerm);
        if (like == null) {
            return SearchQueryUtil.applyDefaultSearchLimit(PageEntity.<PageEntity>findAll()).list();
        }
        return SearchQueryUtil
                .applyDefaultSearchLimit(
                        PageEntity.find("LOWER(title) LIKE ?1 ESCAPE '\\' OR LOWER(content) LIKE ?1 ESCAPE '\\'", like))
                .list();
    }
}
