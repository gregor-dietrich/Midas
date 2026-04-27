package de.vptr.midas.api.dto;

import jakarta.validation.constraints.Size;

/** Data transfer object for post data. */
public class PostDto {

    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    public String title;

    @Size(min = 1, max = 50000, message = "Content must be between 1 and 50000 characters")
    public String content;

    public Long userId;

    public Long categoryId;

    public Boolean published;

    public Boolean commentable;

    public PostDto() {
    }

    /** Creates a new instance. */
    public PostDto(final String title, final String content, final Long userId, final Long categoryId,
            final Boolean published, final Boolean commentable) {
        this.title = title;
        this.content = content;
        this.userId = userId;
        this.categoryId = categoryId;
        this.published = published;
        this.commentable = commentable;
    }
}
