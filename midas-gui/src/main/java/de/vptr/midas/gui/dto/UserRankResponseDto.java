package de.vptr.midas.gui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Response DTO for user rank operations. Contains computed fields and safe data for client responses.
 */
@RegisterForReflection
public class UserRankResponseDto extends AbstractUserRankDto {

    // Computed fields
    @JsonProperty("usersCount")
    private Long usersCount;

    // JUSTIFICATION: Constructor is required by NullAway to initialize fields; cannot be omitted.
    @SuppressWarnings({ "NullAway", "PMD.UnnecessaryConstructor" })
    public UserRankResponseDto() {
        // Default constructor for Jackson deserialization and NullAway compliance
    }

    public Long getUsersCount() {
        return this.usersCount;
    }

    public void setUsersCount(final Long usersCount) {
        this.usersCount = usersCount;
    }

    /** Handles the to user rank dto operation. */
    public UserRankDto toUserRankDto() {
        final UserRankDto dto = new UserRankDto();
        dto.setPublicId(this.getPublicId());
        dto.setName(this.getName());
        dto.setAdminView(this.isAdminView());
        dto.setPageAdd(this.isPageAdd());
        dto.setPageDelete(this.isPageDelete());
        dto.setPageEdit(this.isPageEdit());
        dto.setPostAdd(this.isPostAdd());
        dto.setPostDelete(this.isPostDelete());
        dto.setPostEdit(this.isPostEdit());
        dto.setPostCategoryAdd(this.isPostCategoryAdd());
        dto.setPostCategoryDelete(this.isPostCategoryDelete());
        dto.setPostCategoryEdit(this.isPostCategoryEdit());
        dto.setPostCommentAdd(this.isPostCommentAdd());
        dto.setPostCommentDelete(this.isPostCommentDelete());
        dto.setPostCommentEdit(this.isPostCommentEdit());
        dto.setUserAdd(this.isUserAdd());
        dto.setUserDelete(this.isUserDelete());
        dto.setUserEdit(this.isUserEdit());
        dto.setUserGroupAdd(this.isUserGroupAdd());
        dto.setUserGroupDelete(this.isUserGroupDelete());
        dto.setUserGroupEdit(this.isUserGroupEdit());
        dto.setUserAccountAdd(this.isUserAccountAdd());
        dto.setUserAccountDelete(this.isUserAccountDelete());
        dto.setUserAccountEdit(this.isUserAccountEdit());
        dto.setUserRankAdd(this.isUserRankAdd());
        dto.setUserRankDelete(this.isUserRankDelete());
        dto.setUserRankEdit(this.isUserRankEdit());
        return dto;
    }
}
