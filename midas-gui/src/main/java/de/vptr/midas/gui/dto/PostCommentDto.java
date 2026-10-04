package de.vptr.midas.gui.dto;

/** Data transfer object for post comment. */
public class PostCommentDto extends AbstractPostCommentDto {

    @SuppressWarnings({ "NullAway", "PMD.UnnecessaryConstructor" })
    public PostCommentDto() {
        // Fields are filled by from(), the form Binder or a service
    }

    /** Creates the editable form of a comment the backend returned. */
    public static PostCommentDto from(final PostCommentResponseDto response) {
        final var dto = new PostCommentDto();
        dto.setPublicId(response.getPublicId());
        dto.setContent(response.getContent());
        dto.setPostPublicId(response.getPostPublicId());
        dto.setPostTitle(response.getPostTitle());
        dto.setUserPublicId(response.getUserPublicId());
        dto.setUsername(response.getUsername());
        dto.setCreated(response.getCreated());
        return dto;
    }
}
