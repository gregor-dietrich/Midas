package de.vptr.midas.gui.dto;

/** Data transfer object for post. */
public class PostDto extends AbstractPostDto {

    @SuppressWarnings({ "NullAway", "PMD.UnnecessaryConstructor" })
    public PostDto() {
        // Default constructor for Jackson
    }

    /** Creates the editable form of a post the backend returned. */
    public static PostDto from(final PostResponseDto response) {
        final var dto = new PostDto();
        dto.setPublicId(response.getPublicId());
        dto.setTitle(response.getTitle());
        dto.setContent(response.getContent());
        dto.setUsername(response.getUsername());
        dto.setCategoryPublicId(response.getCategoryPublicId());
        dto.setCategoryName(response.getCategoryName());
        dto.setPublished(response.getPublished());
        dto.setCommentable(response.getCommentable());
        dto.setCreated(response.getCreated());
        dto.setLastEdit(response.getLastEdit());
        dto.setCommentsCount(response.getCommentsCount());
        return dto;
    }
}
