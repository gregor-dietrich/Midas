package de.vptr.midas.gui.dto;

/** Data transfer object for user rank. */
public class UserRankDto extends AbstractUserRankDto {

    public UserRankDto() {
        // Default constructor for Jackson
    }

    public UserRankDto(final String name) {
        this.setName(name);
    }

    /** Creates the editable form of a rank the backend returned; an absent permission counts as not granted. */
    public static UserRankDto from(final UserRankResponseDto response) {
        final var dto = new UserRankDto();
        dto.setPublicId(response.getPublicId());
        dto.setName(response.getName());
        dto.setAdminView(Boolean.TRUE.equals(response.getAdminView()));
        dto.setPageAdd(Boolean.TRUE.equals(response.getPageAdd()));
        dto.setPageDelete(Boolean.TRUE.equals(response.getPageDelete()));
        dto.setPageEdit(Boolean.TRUE.equals(response.getPageEdit()));
        dto.setPostAdd(Boolean.TRUE.equals(response.getPostAdd()));
        dto.setPostDelete(Boolean.TRUE.equals(response.getPostDelete()));
        dto.setPostEdit(Boolean.TRUE.equals(response.getPostEdit()));
        dto.setPostCategoryAdd(Boolean.TRUE.equals(response.getPostCategoryAdd()));
        dto.setPostCategoryDelete(Boolean.TRUE.equals(response.getPostCategoryDelete()));
        dto.setPostCategoryEdit(Boolean.TRUE.equals(response.getPostCategoryEdit()));
        dto.setPostCommentAdd(Boolean.TRUE.equals(response.getPostCommentAdd()));
        dto.setPostCommentDelete(Boolean.TRUE.equals(response.getPostCommentDelete()));
        dto.setPostCommentEdit(Boolean.TRUE.equals(response.getPostCommentEdit()));
        dto.setUserAdd(Boolean.TRUE.equals(response.getUserAdd()));
        dto.setUserDelete(Boolean.TRUE.equals(response.getUserDelete()));
        dto.setUserEdit(Boolean.TRUE.equals(response.getUserEdit()));
        dto.setUserGroupAdd(Boolean.TRUE.equals(response.getUserGroupAdd()));
        dto.setUserGroupDelete(Boolean.TRUE.equals(response.getUserGroupDelete()));
        dto.setUserGroupEdit(Boolean.TRUE.equals(response.getUserGroupEdit()));
        dto.setUserAccountAdd(Boolean.TRUE.equals(response.getUserAccountAdd()));
        dto.setUserAccountDelete(Boolean.TRUE.equals(response.getUserAccountDelete()));
        dto.setUserAccountEdit(Boolean.TRUE.equals(response.getUserAccountEdit()));
        dto.setUserRankAdd(Boolean.TRUE.equals(response.getUserRankAdd()));
        dto.setUserRankDelete(Boolean.TRUE.equals(response.getUserRankDelete()));
        dto.setUserRankEdit(Boolean.TRUE.equals(response.getUserRankEdit()));
        return dto;
    }
}
