package de.vptr.midas.api.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Data transfer object for post data. */
public class PostDto {

    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    @Nullable
    public String title;

    @NotBlank(message = "Content is required")
    @Size(min = 1, max = 50000, message = "Content must be between 1 and 50000 characters")
    @Nullable
    public String content;

    @SuppressWarnings("MultipleNullnessAnnotations")
    @NotNull(message = "User ID is required")
    @Nullable
    public Long userId;

    @Nullable
    public Long categoryId;

    @Nullable
    public Boolean published;

    @Nullable
    public Boolean commentable;

    public PostDto() {
    }

    /** Creates a new instance. */
    public PostDto(@Nullable final String title, @Nullable final String content, @Nullable final Long userId,
            @Nullable final Long categoryId, @Nullable final Boolean published, @Nullable final Boolean commentable) {
        this.title = title;
        this.content = content;
        this.userId = userId;
        this.categoryId = categoryId;
        this.published = published;
        this.commentable = commentable;
    }
}
