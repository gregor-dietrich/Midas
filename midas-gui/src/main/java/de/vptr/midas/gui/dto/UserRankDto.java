package de.vptr.midas.gui.dto;

import jakarta.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Data transfer object for user rank. */
// 25 permission flags require getters, setters and convenience methods (91 methods, max CC 1).
@SuppressWarnings({ "PMD.ExcessivePublicCount", "PMD.CyclomaticComplexity" })
public class UserRankDto {

    @JsonProperty("publicId")
    @Nullable
    private String publicId;

    @JsonProperty("name")
    @Nullable
    private String name;

    // View permissions
    @JsonProperty("adminView")
    private Boolean adminView = false;

    // Page permissions
    @JsonProperty("pageAdd")
    private Boolean pageAdd = false;

    @JsonProperty("pageDelete")
    private Boolean pageDelete = false;

    @JsonProperty("pageEdit")
    private Boolean pageEdit = false;

    // Post permissions
    @JsonProperty("postAdd")
    private Boolean postAdd = false;

    @JsonProperty("postDelete")
    private Boolean postDelete = false;

    @JsonProperty("postEdit")
    private Boolean postEdit = false;

    // Post category permissions
    @JsonProperty("postCategoryAdd")
    private Boolean postCategoryAdd = false;

    @JsonProperty("postCategoryDelete")
    private Boolean postCategoryDelete = false;

    @JsonProperty("postCategoryEdit")
    private Boolean postCategoryEdit = false;

    // Post comment permissions
    @JsonProperty("postCommentAdd")
    private Boolean postCommentAdd = false;

    @JsonProperty("postCommentDelete")
    private Boolean postCommentDelete = false;

    @JsonProperty("postCommentEdit")
    private Boolean postCommentEdit = false;

    // User permissions
    @JsonProperty("userAdd")
    private Boolean userAdd = false;

    @JsonProperty("userDelete")
    private Boolean userDelete = false;

    @JsonProperty("userEdit")
    private Boolean userEdit = false;

    // User group permissions
    @JsonProperty("userGroupAdd")
    private Boolean userGroupAdd = false;

    @JsonProperty("userGroupDelete")
    private Boolean userGroupDelete = false;

    @JsonProperty("userGroupEdit")
    private Boolean userGroupEdit = false;

    // Account permissions
    @JsonProperty("userAccountAdd")
    private Boolean userAccountAdd = false;

    @JsonProperty("userAccountDelete")
    private Boolean userAccountDelete = false;

    @JsonProperty("userAccountEdit")
    private Boolean userAccountEdit = false;

    // User rank permissions
    @JsonProperty("userRankAdd")
    private Boolean userRankAdd = false;

    @JsonProperty("userRankDelete")
    private Boolean userRankDelete = false;

    @JsonProperty("userRankEdit")
    private Boolean userRankEdit = false;

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

    // Getter methods
    @Nullable
    public String getPublicId() {
        return this.publicId;
    }

    @Nullable
    public String getName() {
        return this.name;
    }

    // View permissions getters
    public Boolean isAdminView() {
        return this.adminView;
    }

    // Page permissions getters
    public Boolean isPageAdd() {
        return this.pageAdd;
    }

    public Boolean isPageDelete() {
        return this.pageDelete;
    }

    public Boolean isPageEdit() {
        return this.pageEdit;
    }

    // Post permissions getters
    public Boolean isPostAdd() {
        return this.postAdd;
    }

    public Boolean isPostDelete() {
        return this.postDelete;
    }

    public Boolean isPostEdit() {
        return this.postEdit;
    }

    // Post category permissions getters
    public Boolean isPostCategoryAdd() {
        return this.postCategoryAdd;
    }

    public Boolean isPostCategoryDelete() {
        return this.postCategoryDelete;
    }

    public Boolean isPostCategoryEdit() {
        return this.postCategoryEdit;
    }

    // Post comment permissions getters
    public Boolean isPostCommentAdd() {
        return this.postCommentAdd;
    }

    public Boolean isPostCommentDelete() {
        return this.postCommentDelete;
    }

    public Boolean isPostCommentEdit() {
        return this.postCommentEdit;
    }

    // User permissions getters
    public Boolean isUserAdd() {
        return this.userAdd;
    }

    public Boolean isUserDelete() {
        return this.userDelete;
    }

    public Boolean isUserEdit() {
        return this.userEdit;
    }

    // User group permissions getters
    public Boolean isUserGroupAdd() {
        return this.userGroupAdd;
    }

    public Boolean isUserGroupDelete() {
        return this.userGroupDelete;
    }

    public Boolean isUserGroupEdit() {
        return this.userGroupEdit;
    }

    // Account permissions getters
    public Boolean isUserAccountAdd() {
        return this.userAccountAdd;
    }

    public Boolean isUserAccountDelete() {
        return this.userAccountDelete;
    }

    public Boolean isUserAccountEdit() {
        return this.userAccountEdit;
    }

    // User rank permissions getters
    public Boolean isUserRankAdd() {
        return this.userRankAdd;
    }

    public Boolean isUserRankDelete() {
        return this.userRankDelete;
    }

    public Boolean isUserRankEdit() {
        return this.userRankEdit;
    }

    // Setter methods
    public void setPublicId(@Nullable final String publicId) {
        this.publicId = publicId;
    }

    public void setName(@Nullable final String name) {
        this.name = name;
    }

    // View permissions setters
    public void setAdminView(final Boolean adminView) {
        this.adminView = adminView;
    }

    // Page permissions setters
    public void setPageAdd(final Boolean pageAdd) {
        this.pageAdd = pageAdd;
    }

    public void setPageDelete(final Boolean pageDelete) {
        this.pageDelete = pageDelete;
    }

    public void setPageEdit(final Boolean pageEdit) {
        this.pageEdit = pageEdit;
    }

    // Post permissions setters
    public void setPostAdd(final Boolean postAdd) {
        this.postAdd = postAdd;
    }

    public void setPostDelete(final Boolean postDelete) {
        this.postDelete = postDelete;
    }

    public void setPostEdit(final Boolean postEdit) {
        this.postEdit = postEdit;
    }

    // Post category permissions setters
    public void setPostCategoryAdd(final Boolean postCategoryAdd) {
        this.postCategoryAdd = postCategoryAdd;
    }

    public void setPostCategoryDelete(final Boolean postCategoryDelete) {
        this.postCategoryDelete = postCategoryDelete;
    }

    public void setPostCategoryEdit(final Boolean postCategoryEdit) {
        this.postCategoryEdit = postCategoryEdit;
    }

    // Post comment permissions setters
    public void setPostCommentAdd(final Boolean postCommentAdd) {
        this.postCommentAdd = postCommentAdd;
    }

    public void setPostCommentDelete(final Boolean postCommentDelete) {
        this.postCommentDelete = postCommentDelete;
    }

    public void setPostCommentEdit(final Boolean postCommentEdit) {
        this.postCommentEdit = postCommentEdit;
    }

    // User permissions setters
    public void setUserAdd(final Boolean userAdd) {
        this.userAdd = userAdd;
    }

    public void setUserDelete(final Boolean userDelete) {
        this.userDelete = userDelete;
    }

    public void setUserEdit(final Boolean userEdit) {
        this.userEdit = userEdit;
    }

    // User group permissions setters
    public void setUserGroupAdd(final Boolean userGroupAdd) {
        this.userGroupAdd = userGroupAdd;
    }

    public void setUserGroupDelete(final Boolean userGroupDelete) {
        this.userGroupDelete = userGroupDelete;
    }

    public void setUserGroupEdit(final Boolean userGroupEdit) {
        this.userGroupEdit = userGroupEdit;
    }

    // Account permissions setters
    public void setUserAccountAdd(final Boolean userAccountAdd) {
        this.userAccountAdd = userAccountAdd;
    }

    public void setUserAccountDelete(final Boolean userAccountDelete) {
        this.userAccountDelete = userAccountDelete;
    }

    public void setUserAccountEdit(final Boolean userAccountEdit) {
        this.userAccountEdit = userAccountEdit;
    }

    // User rank permissions setters
    public void setUserRankAdd(final Boolean userRankAdd) {
        this.userRankAdd = userRankAdd;
    }

    public void setUserRankDelete(final Boolean userRankDelete) {
        this.userRankDelete = userRankDelete;
    }

    public void setUserRankEdit(final Boolean userRankEdit) {
        this.userRankEdit = userRankEdit;
    }

    // Convenience methods for checking permissions
    public boolean canAdminView() {
        return Boolean.TRUE.equals(this.adminView);
    }

    public boolean canAddPage() {
        return Boolean.TRUE.equals(this.pageAdd);
    }

    public boolean canDeletePage() {
        return Boolean.TRUE.equals(this.pageDelete);
    }

    public boolean canEditPage() {
        return Boolean.TRUE.equals(this.pageEdit);
    }

    public boolean canAddPost() {
        return Boolean.TRUE.equals(this.postAdd);
    }

    public boolean canDeletePost() {
        return Boolean.TRUE.equals(this.postDelete);
    }

    public boolean canEditPost() {
        return Boolean.TRUE.equals(this.postEdit);
    }

    public boolean canAddPostCategory() {
        return Boolean.TRUE.equals(this.postCategoryAdd);
    }

    public boolean canDeletePostCategory() {
        return Boolean.TRUE.equals(this.postCategoryDelete);
    }

    public boolean canEditPostCategory() {
        return Boolean.TRUE.equals(this.postCategoryEdit);
    }

    public boolean canAddPostComment() {
        return Boolean.TRUE.equals(this.postCommentAdd);
    }

    public boolean canDeletePostComment() {
        return Boolean.TRUE.equals(this.postCommentDelete);
    }

    public boolean canEditPostComment() {
        return Boolean.TRUE.equals(this.postCommentEdit);
    }

    public boolean canAddUser() {
        return Boolean.TRUE.equals(this.userAdd);
    }

    public boolean canDeleteUser() {
        return Boolean.TRUE.equals(this.userDelete);
    }

    public boolean canEditUser() {
        return Boolean.TRUE.equals(this.userEdit);
    }

    public boolean canAddUserGroup() {
        return Boolean.TRUE.equals(this.userGroupAdd);
    }

    public boolean canDeleteUserGroup() {
        return Boolean.TRUE.equals(this.userGroupDelete);
    }

    public boolean canEditUserGroup() {
        return Boolean.TRUE.equals(this.userGroupEdit);
    }

    public boolean canAddUserAccount() {
        return Boolean.TRUE.equals(this.userAccountAdd);
    }

    public boolean canDeleteUserAccount() {
        return Boolean.TRUE.equals(this.userAccountDelete);
    }

    public boolean canEditUserAccount() {
        return Boolean.TRUE.equals(this.userAccountEdit);
    }

    public boolean canAddUserRank() {
        return Boolean.TRUE.equals(this.userRankAdd);
    }

    public boolean canDeleteUserRank() {
        return Boolean.TRUE.equals(this.userRankDelete);
    }

    public boolean canEditUserRank() {
        return Boolean.TRUE.equals(this.userRankEdit);
    }

    /** Returns whether any permission is granted. */
    public boolean hasAnyPermission() {
        return this.canAdminView() || this.hasAnyPagePermission() || this.hasAnyPostPermission()
                || this.hasAnyPostCategoryPermission() || this.hasAnyPostCommentPermission()
                || this.hasAnyUserPermission() || this.hasAnyUserGroupPermission() || this.hasAnyUserAccountPermission()
                || this.hasAnyUserRankPermission();
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
}
