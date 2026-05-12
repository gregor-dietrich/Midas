package de.vptr.midas.gui.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Response DTO for post operations. Contains computed fields and safe data for client responses.
 */
@RegisterForReflection
public class PostResponseDto extends AbstractPostDto {

    @SuppressWarnings({ "NullAway", "PMD.UnnecessaryConstructor" })
    public PostResponseDto() {
        // Default constructor for Jackson
    }

    /** Handles the to post dto operation. */
    public PostDto toPostDto() {
        final PostDto dto = new PostDto();
        dto.setId(this.getId());
        dto.setTitle(this.getTitle());
        dto.setContent(this.getContent());
        dto.setUserId(this.getUserId());
        dto.setUsername(this.getUsername());
        dto.setCategoryId(this.getCategoryId());
        dto.setCategoryName(this.getCategoryName());
        dto.setPublished(this.isPublished());
        dto.setCommentable(this.isCommentable());
        dto.setCreated(this.getCreated());
        dto.setLastEdit(this.getLastEdit());
        dto.setCommentsCount(this.getCommentsCount());
        return dto;
    }
}
