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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/** JPA entity representing a post category. */
@Entity
@Table(name = "post_categories", indexes = {
        @Index(name = "idx_post_category_parent_id", columnList = "parent_id")
})
public class PostCategoryEntity extends PanacheEntityBase {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    public PostCategoryEntity parent;

    @JsonIgnore
    @OneToMany(mappedBy = "parent", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    public List<PostCategoryEntity> children;

    @JsonIgnore
    @OneToMany(mappedBy = "category", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    public List<PostEntity> posts;

    // Helper method to check if this is a root category
    /** Returns true if this category has no parent. */
    public boolean isRootCategory() {
        return this.parent == null;
    }

    // Helper method to get all subcategories recursively
    /** Returns by parent id. */
    public static List<PostCategoryEntity> findByParentId(final Long parentId) {
        if (parentId == null) {
            return find("parent IS NULL").list();
        }
        return find("parent.id", parentId).list();
    }

    // Helper method to find root categories
    public static List<PostCategoryEntity> findRootCategories() {
        return find("parent IS NULL").list();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final PostCategoryEntity that = (PostCategoryEntity) o;
        return this.id != null && this.id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hashCode(this.id);
    }
}
