package de.vptr.midas.api.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.annotation.Nullable;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

/** JPA entity representing a post category. */
@Entity
@Table(name = "post_categories")
public class PostCategoryEntity extends BaseEntity {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @Nullable
    public PostCategoryEntity parent;

    @JsonIgnore
    @OneToMany(mappedBy = "parent", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @Nullable
    public List<PostCategoryEntity> children;

    @JsonIgnore
    @OneToMany(mappedBy = "category", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @Nullable
    public List<PostEntity> posts;

    // Helper method to check if this is a root category
    /** Returns true if this category has no parent. */
    public boolean isRootCategory() {
        return this.parent == null;
    }

    // Helper method to get all subcategories recursively
    /** Returns by parent id. */
    public static List<PostCategoryEntity> findByParentId(@Nullable final Long parentId) {
        if (parentId == null) {
            return find("parent IS NULL").list();
        }
        return find("parent.id", parentId).list();
    }

    // Helper method to find root categories
    public static List<PostCategoryEntity> findRootCategories() {
        return find("parent IS NULL").list();
    }
}
