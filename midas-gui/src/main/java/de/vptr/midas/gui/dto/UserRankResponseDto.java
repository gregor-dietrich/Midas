package de.vptr.midas.gui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Response DTO for user rank operations. Contains computed fields and safe data for client responses.
 */
@RegisterForReflection
public class UserRankResponseDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    // View permissions
    @JsonProperty("adminView")
    private Boolean adminView;

    // Page permissions
    @JsonProperty("pageAdd")
    private Boolean pageAdd;

    @JsonProperty("pageDelete")
    private Boolean pageDelete;

    @JsonProperty("pageEdit")
    private Boolean pageEdit;

    // Post permissions
    @JsonProperty("postAdd")
    private Boolean postAdd;

    @JsonProperty("postDelete")
    private Boolean postDelete;

    @JsonProperty("postEdit")
    private Boolean postEdit;

    // Post category permissions
    @JsonProperty("postCategoryAdd")
    private Boolean postCategoryAdd;

    @JsonProperty("postCategoryDelete")
    private Boolean postCategoryDelete;

    @JsonProperty("postCategoryEdit")
    private Boolean postCategoryEdit;

    // Post comment permissions
    @JsonProperty("postCommentAdd")
    private Boolean postCommentAdd;

    @JsonProperty("postCommentDelete")
    private Boolean postCommentDelete;

    @JsonProperty("postCommentEdit")
    private Boolean postCommentEdit;

    // User permissions
    @JsonProperty("userAdd")
    private Boolean userAdd;

    @JsonProperty("userDelete")
    private Boolean userDelete;

    @JsonProperty("userEdit")
    private Boolean userEdit;

    // User group permissions
    @JsonProperty("userGroupAdd")
    private Boolean userGroupAdd;

    @JsonProperty("userGroupDelete")
    private Boolean userGroupDelete;

    @JsonProperty("userGroupEdit")
    private Boolean userGroupEdit;

    // Account permissions
    @JsonProperty("userAccountAdd")
    private Boolean userAccountAdd;

    @JsonProperty("userAccountDelete")
    private Boolean userAccountDelete;

    @JsonProperty("userAccountEdit")
    private Boolean userAccountEdit;

    // User rank permissions
    @JsonProperty("userRankAdd")
    private Boolean userRankAdd;

    @JsonProperty("userRankDelete")
    private Boolean userRankDelete;

    @JsonProperty("userRankEdit")
    private Boolean userRankEdit;

    // Computed fields
    @JsonProperty("usersCount")
    private Long usersCount;

    @SuppressWarnings("NullAway")
    public UserRankResponseDto() {
        // Default constructor for Jackson
    }

    // Getters
    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Boolean getAdminView() {
        return this.adminView;
    }

    public Boolean getPageAdd() {
        return this.pageAdd;
    }

    public Boolean getPageDelete() {
        return this.pageDelete;
    }

    public Boolean getPageEdit() {
        return this.pageEdit;
    }

    public Boolean getPostAdd() {
        return this.postAdd;
    }

    public Boolean getPostDelete() {
        return this.postDelete;
    }

    public Boolean getPostEdit() {
        return this.postEdit;
    }

    public Boolean getPostCategoryAdd() {
        return this.postCategoryAdd;
    }

    public Boolean getPostCategoryDelete() {
        return this.postCategoryDelete;
    }

    public Boolean getPostCategoryEdit() {
        return this.postCategoryEdit;
    }

    public Boolean getPostCommentAdd() {
        return this.postCommentAdd;
    }

    public Boolean getPostCommentDelete() {
        return this.postCommentDelete;
    }

    public Boolean getPostCommentEdit() {
        return this.postCommentEdit;
    }

    public Boolean getUserAdd() {
        return this.userAdd;
    }

    public Boolean getUserDelete() {
        return this.userDelete;
    }

    public Boolean getUserEdit() {
        return this.userEdit;
    }

    public Boolean getUserGroupAdd() {
        return this.userGroupAdd;
    }

    public Boolean getUserGroupDelete() {
        return this.userGroupDelete;
    }

    public Boolean getUserGroupEdit() {
        return this.userGroupEdit;
    }

    public Boolean getUserAccountAdd() {
        return this.userAccountAdd;
    }

    public Boolean getUserAccountDelete() {
        return this.userAccountDelete;
    }

    public Boolean getUserAccountEdit() {
        return this.userAccountEdit;
    }

    public Boolean getUserRankAdd() {
        return this.userRankAdd;
    }

    public Boolean getUserRankDelete() {
        return this.userRankDelete;
    }

    public Boolean getUserRankEdit() {
        return this.userRankEdit;
    }

    public Long getUsersCount() {
        return this.usersCount;
    }

    // Setters
    public void setId(final Long id) {
        this.id = id;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setAdminView(final Boolean adminView) {
        this.adminView = adminView;
    }

    public void setPageAdd(final Boolean pageAdd) {
        this.pageAdd = pageAdd;
    }

    public void setPageDelete(final Boolean pageDelete) {
        this.pageDelete = pageDelete;
    }

    public void setPageEdit(final Boolean pageEdit) {
        this.pageEdit = pageEdit;
    }

    public void setPostAdd(final Boolean postAdd) {
        this.postAdd = postAdd;
    }

    public void setPostDelete(final Boolean postDelete) {
        this.postDelete = postDelete;
    }

    public void setPostEdit(final Boolean postEdit) {
        this.postEdit = postEdit;
    }

    public void setPostCategoryAdd(final Boolean postCategoryAdd) {
        this.postCategoryAdd = postCategoryAdd;
    }

    public void setPostCategoryDelete(final Boolean postCategoryDelete) {
        this.postCategoryDelete = postCategoryDelete;
    }

    public void setPostCategoryEdit(final Boolean postCategoryEdit) {
        this.postCategoryEdit = postCategoryEdit;
    }

    public void setPostCommentAdd(final Boolean postCommentAdd) {
        this.postCommentAdd = postCommentAdd;
    }

    public void setPostCommentDelete(final Boolean postCommentDelete) {
        this.postCommentDelete = postCommentDelete;
    }

    public void setPostCommentEdit(final Boolean postCommentEdit) {
        this.postCommentEdit = postCommentEdit;
    }

    public void setUserAdd(final Boolean userAdd) {
        this.userAdd = userAdd;
    }

    public void setUserDelete(final Boolean userDelete) {
        this.userDelete = userDelete;
    }

    public void setUserEdit(final Boolean userEdit) {
        this.userEdit = userEdit;
    }

    public void setUserGroupAdd(final Boolean userGroupAdd) {
        this.userGroupAdd = userGroupAdd;
    }

    public void setUserGroupDelete(final Boolean userGroupDelete) {
        this.userGroupDelete = userGroupDelete;
    }

    public void setUserGroupEdit(final Boolean userGroupEdit) {
        this.userGroupEdit = userGroupEdit;
    }

    public void setUserAccountAdd(final Boolean userAccountAdd) {
        this.userAccountAdd = userAccountAdd;
    }

    public void setUserAccountDelete(final Boolean userAccountDelete) {
        this.userAccountDelete = userAccountDelete;
    }

    public void setUserAccountEdit(final Boolean userAccountEdit) {
        this.userAccountEdit = userAccountEdit;
    }

    public void setUserRankAdd(final Boolean userRankAdd) {
        this.userRankAdd = userRankAdd;
    }

    public void setUserRankDelete(final Boolean userRankDelete) {
        this.userRankDelete = userRankDelete;
    }

    public void setUserRankEdit(final Boolean userRankEdit) {
        this.userRankEdit = userRankEdit;
    }

    public void setUsersCount(final Long usersCount) {
        this.usersCount = usersCount;
    }

    // Convenience methods for checking permissions
    public boolean canAdminView() {
        return this.adminView != null && this.adminView;
    }

    public boolean canAddPage() {
        return this.pageAdd != null && this.pageAdd;
    }

    public boolean canDeletePage() {
        return this.pageDelete != null && this.pageDelete;
    }

    public boolean canEditPage() {
        return this.pageEdit != null && this.pageEdit;
    }

    public boolean canAddPost() {
        return this.postAdd != null && this.postAdd;
    }

    public boolean canDeletePost() {
        return this.postDelete != null && this.postDelete;
    }

    public boolean canEditPost() {
        return this.postEdit != null && this.postEdit;
    }

    public boolean canAddPostCategory() {
        return this.postCategoryAdd != null && this.postCategoryAdd;
    }

    public boolean canDeletePostCategory() {
        return this.postCategoryDelete != null && this.postCategoryDelete;
    }

    public boolean canEditPostCategory() {
        return this.postCategoryEdit != null && this.postCategoryEdit;
    }

    public boolean canAddPostComment() {
        return this.postCommentAdd != null && this.postCommentAdd;
    }

    public boolean canDeletePostComment() {
        return this.postCommentDelete != null && this.postCommentDelete;
    }

    public boolean canEditPostComment() {
        return this.postCommentEdit != null && this.postCommentEdit;
    }

    public boolean canAddUser() {
        return this.userAdd != null && this.userAdd;
    }

    public boolean canDeleteUser() {
        return this.userDelete != null && this.userDelete;
    }

    public boolean canEditUser() {
        return this.userEdit != null && this.userEdit;
    }

    public boolean canAddUserGroup() {
        return this.userGroupAdd != null && this.userGroupAdd;
    }

    public boolean canDeleteUserGroup() {
        return this.userGroupDelete != null && this.userGroupDelete;
    }

    public boolean canEditUserGroup() {
        return this.userGroupEdit != null && this.userGroupEdit;
    }

    public boolean canAddUserAccount() {
        return this.userAccountAdd != null && this.userAccountAdd;
    }

    public boolean canDeleteUserAccount() {
        return this.userAccountDelete != null && this.userAccountDelete;
    }

    public boolean canEditUserAccount() {
        return this.userAccountEdit != null && this.userAccountEdit;
    }

    public boolean canAddUserRank() {
        return this.userRankAdd != null && this.userRankAdd;
    }

    public boolean canDeleteUserRank() {
        return this.userRankDelete != null && this.userRankDelete;
    }

    public boolean canEditUserRank() {
        return this.userRankEdit != null && this.userRankEdit;
    }

    public boolean hasAnyPagePermission() {
        return this.canAddPage() || this.canEditPage() || this.canDeletePage();
    }

    public boolean hasAnyPostPermission() {
        return this.canAddPost() || this.canEditPost() || this.canDeletePost();
    }

    public boolean hasAnyPostCategoryPermission() {
        return this.canAddPostCategory() || this.canEditPostCategory() || this.canDeletePostCategory();
    }

    public boolean hasAnyPostCommentPermission() {
        return this.canAddPostComment() || this.canEditPostComment() || this.canDeletePostComment();
    }

    public boolean hasAnyUserPermission() {
        return this.canAddUser() || this.canEditUser() || this.canDeleteUser();
    }

    public boolean hasAnyUserGroupPermission() {
        return this.canAddUserGroup() || this.canEditUserGroup() || this.canDeleteUserGroup();
    }

    public boolean hasAnyUserAccountPermission() {
        return this.canAddUserAccount() || this.canEditUserAccount() || this.canDeleteUserAccount();
    }

    public boolean hasAnyUserRankPermission() {
        return this.canAddUserRank() || this.canEditUserRank() || this.canDeleteUserRank();
    }

    /** Returns whether any permission is granted. */
    public boolean hasAnyPermission() {
        return this.canAdminView() || this.hasAnyPagePermission() || this.hasAnyPostPermission()
                || this.hasAnyPostCategoryPermission() || this.hasAnyPostCommentPermission()
                || this.hasAnyUserPermission() || this.hasAnyUserGroupPermission() || this.hasAnyUserAccountPermission()
                || this.hasAnyUserRankPermission();
    }

    /** Handles the to user rank dto operation. */
    public UserRankDto toUserRankDto() {
        final UserRankDto dto = new UserRankDto();
        dto.setId(this.id);
        dto.setName(this.name);
        dto.setAdminView(this.adminView);
        dto.setPageAdd(this.pageAdd);
        dto.setPageDelete(this.pageDelete);
        dto.setPageEdit(this.pageEdit);
        dto.setPostAdd(this.postAdd);
        dto.setPostDelete(this.postDelete);
        dto.setPostEdit(this.postEdit);
        dto.setPostCategoryAdd(this.postCategoryAdd);
        dto.setPostCategoryDelete(this.postCategoryDelete);
        dto.setPostCategoryEdit(this.postCategoryEdit);
        dto.setPostCommentAdd(this.postCommentAdd);
        dto.setPostCommentDelete(this.postCommentDelete);
        dto.setPostCommentEdit(this.postCommentEdit);
        dto.setUserAdd(this.userAdd);
        dto.setUserDelete(this.userDelete);
        dto.setUserEdit(this.userEdit);
        dto.setUserGroupAdd(this.userGroupAdd);
        dto.setUserGroupDelete(this.userGroupDelete);
        dto.setUserGroupEdit(this.userGroupEdit);
        dto.setUserAccountAdd(this.userAccountAdd);
        dto.setUserAccountDelete(this.userAccountDelete);
        dto.setUserAccountEdit(this.userAccountEdit);
        dto.setUserRankAdd(this.userRankAdd);
        dto.setUserRankDelete(this.userRankDelete);
        dto.setUserRankEdit(this.userRankEdit);
        return dto;
    }

}
