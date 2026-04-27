package de.vptr.midas.api.dto;

import java.time.LocalDateTime;

import de.vptr.midas.api.entity.PostCommentEntity;

/** Data transfer object for post comment response data. */
public class PostCommentResponseDto {
    public Long id;
    public String content;
    public Long postId;
    public String postTitle;
    public Long userId;
    public String username;
    public LocalDateTime created;
    public LocalDateTime lastEdit;

    public PostCommentResponseDto() {
    }

    /** Creates a new instance. */
    public PostCommentResponseDto(final PostCommentEntity entity) {
        this.id = entity.id;
        this.content = entity.content;
        this.created = entity.created;

        if (entity.post != null) {
            this.postId = entity.post.id;
            this.postTitle = entity.post.title;
        }

        if (entity.user != null) {
            this.userId = entity.user.id;
            this.username = entity.user.username;
        }
        this.lastEdit = entity.lastEdit;
    }
}
