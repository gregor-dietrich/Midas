package de.vptr.midas.api.dto;

import java.time.LocalDateTime;

import de.vptr.midas.api.entity.PostEntity;
import jakarta.annotation.Nullable;

/** Data transfer object for post response data. */
public class PostResponseDto {

    @Nullable
    public Long id;
    @Nullable
    public String title;
    @Nullable
    public String content;
    @Nullable
    public Long userId;
    @Nullable
    public String username;
    @Nullable
    public Long categoryId;
    @Nullable
    public String categoryName;
    @Nullable
    public Boolean published;
    @Nullable
    public Boolean commentable;
    @Nullable
    public LocalDateTime created;
    @Nullable
    public LocalDateTime lastEdit;
    @Nullable
    public Long commentsCount;

    public PostResponseDto() {
    }

    /**
     * Creates a new instance with pre-computed comment count (avoids lazy-loading collections).
     */
    public PostResponseDto(@Nullable final PostEntity entity, final long commentsCount) {
        if (entity != null) {
            this.id = entity.id;
            this.title = entity.title;
            this.content = entity.content;
            this.userId = entity.user != null ? entity.user.id : null;
            this.username = entity.user != null ? entity.user.username : null;
            this.categoryId = entity.category != null ? entity.category.id : null;
            this.categoryName = entity.category != null ? entity.category.name : null;
            this.published = entity.published;
            this.commentable = entity.commentable;
            this.created = entity.created;
            this.lastEdit = entity.lastEdit;
            this.commentsCount = commentsCount;
        }
    }
}
