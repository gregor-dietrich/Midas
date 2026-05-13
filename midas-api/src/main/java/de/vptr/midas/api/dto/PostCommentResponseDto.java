package de.vptr.midas.api.dto;

import java.time.LocalDateTime;

import de.vptr.midas.api.entity.PostCommentEntity;
import jakarta.annotation.Nullable;

/** Data transfer object for post comment response data. */
public class PostCommentResponseDto {
    @Nullable
    public String publicId;
    @Nullable
    public String content;
    @Nullable
    public String postPublicId;
    @Nullable
    public String postTitle;
    @Nullable
    public String userPublicId;
    @Nullable
    public String username;
    @Nullable
    public LocalDateTime created;
    @Nullable
    public LocalDateTime lastEdit;

    public PostCommentResponseDto() {
    }

    /** Creates a new instance. */
    public PostCommentResponseDto(final PostCommentEntity entity) {
        this.publicId = entity.publicId;
        this.content = entity.content;
        this.created = entity.created;

        if (entity.post != null) {
            this.postPublicId = entity.post.publicId;
            this.postTitle = entity.post.title;
        }

        if (entity.user != null) {
            this.userPublicId = entity.user.publicId;
            this.username = entity.user.username;
        }
        this.lastEdit = entity.lastEdit;
    }
}
