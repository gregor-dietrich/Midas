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
    public String publicId;
    @Nullable
    public String name;
    @Nullable
    public String parentPublicId;
    @Nullable
    public String parentName;
    public boolean isRootCategory;
    public int childrenCount;
    public int postsCount;
    @Nullable
    public List<String> childrenPublicIds;
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
            @Nullable final List<String> childrenPublicIds, final int postsCount) {
        this.publicId = entity.publicId;
        this.name = entity.name;
        this.isRootCategory = entity.isRootCategory();

        if (entity.parent != null) {
            this.parentPublicId = entity.parent.publicId;
            this.parentName = entity.parent.name;
        }

        this.childrenCount = childrenCount;
        this.childrenPublicIds = childrenPublicIds != null ? childrenPublicIds : List.of();
        this.postsCount = postsCount;
        this.created = entity.created;
        this.lastEdit = entity.lastEdit;
    }
}
