package de.vptr.midas.api.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for post comment operations (POST, PUT, PATCH).
 *
 * - POST: content required (validated by service), postId required - PUT: content required (validated by service),
 * postId ignored (from URL) - PATCH: content optional (allows null), postId ignored (from URL)
 */
public class PostCommentDto {

    @NotBlank(message = "Content is required")
    @Size(min = 1, max = 10_000, message = "Content must be between 1 and 10_000 characters")
    @Nullable
    public String content;

    // Required for POST operations (creation); null is allowed for PUT/PATCH.
    @Nullable
    public String postPublicId;

}
