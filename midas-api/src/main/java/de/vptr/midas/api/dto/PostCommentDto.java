package de.vptr.midas.api.dto;

import jakarta.validation.constraints.Size;

/**
 * DTO for post comment operations (POST, PUT, PATCH).
 * 
 * - POST: content required (validated by service), postId required
 * - PUT: content required (validated by service), postId ignored (from URL)
 * - PATCH: content optional (allows null), postId ignored (from URL)
 */
public class PostCommentDto {

    @Size(min = 1, max = 10000, message = "Content must be between 1 and 10000 characters when provided")
    public String content;

    // Required for POST operations (creation); null is allowed for PUT/PATCH.
    public Long postId;

}
