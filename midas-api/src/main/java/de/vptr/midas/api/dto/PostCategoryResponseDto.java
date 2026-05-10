package de.vptr.midas.api.dto;

import java.time.LocalDateTime;
import java.util.List;

import de.vptr.midas.api.entity.PostCategoryEntity;
import jakarta.annotation.Nullable;

/**
 * Response DTO for post category operations. Contains computed fields and safe data for client responses.
 */
public class PostCategoryResponseDto {

    @Nullable
    public Long id;
    @Nullable
    public String name;
    @Nullable
    public Long parentId;
    @Nullable
    public String parentName;
    public boolean isRootCategory;
    public int childrenCount;
    public int postsCount;
    @Nullable
    public List<Long> childrenIds;
    @Nullable
    public LocalDateTime created;
    @Nullable
    public LocalDateTime lastEdit;

    public PostCategoryResponseDto() {
        // Default constructor for Jackson
    }

    /**
     * Creates a new instance with pre-computed counts (avoids lazy-loading collections).
     */
    public PostCategoryResponseDto(final PostCategoryEntity entity, final int childrenCount,
            @Nullable final List<Long> childrenIds, final int postsCount) {
        this.id = entity.id;
        this.name = entity.name;
        this.isRootCategory = entity.isRootCategory();

        if (entity.parent != null) {
            this.parentId = entity.parent.id;
            this.parentName = entity.parent.name;
        }

        this.childrenCount = childrenCount;
        this.childrenIds = childrenIds != null ? childrenIds : List.of();
        this.postsCount = postsCount;
        this.created = entity.created;
        this.lastEdit = entity.lastEdit;
    }
}
