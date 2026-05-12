package de.vptr.midas.gui.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Response DTO for post comment operations. Contains computed fields and safe data for client responses.
 */
@RegisterForReflection
public class PostCommentResponseDto extends AbstractPostCommentDto {

    @SuppressWarnings({ "NullAway", "PMD.UnnecessaryConstructor" })
    public PostCommentResponseDto() {
        // Default constructor for Jackson
    }

    /** Handles the to post comment dto operation. */
    public PostCommentDto toPostCommentDto() {
        final PostCommentDto dto = new PostCommentDto();
        dto.setId(this.getId());
        dto.setContent(this.getContent());
        dto.setPostId(this.getPostId());
        dto.setPostTitle(this.getPostTitle());
        dto.setUserId(this.getUserId());
        dto.setUsername(this.getUsername());
        dto.setCreated(this.getCreated());
        return dto;
    }
}
