package de.vptr.midas.gui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Data transfer object for user rank. */
public class UserRankDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
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
        this.name = name;
    }

    // Getter methods
    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    // View permissions getters
    public Boolean getAdminView() {
        return this.adminView;
    }

    // Page permissions getters
    public Boolean getPageAdd() {
        return this.pageAdd;
    }

    public Boolean getPageDelete() {
        return this.pageDelete;
    }

    public Boolean getPageEdit() {
        return this.pageEdit;
    }

    // Post permissions getters
    public Boolean getPostAdd() {
        return this.postAdd;
    }

    public Boolean getPostDelete() {
        return this.postDelete;
    }

    public Boolean getPostEdit() {
        return this.postEdit;
    }

    // Post category permissions getters
    public Boolean getPostCategoryAdd() {
        return this.postCategoryAdd;
    }

    public Boolean getPostCategoryDelete() {
        return this.postCategoryDelete;
    }

    public Boolean getPostCategoryEdit() {
        return this.postCategoryEdit;
    }

    // Post comment permissions getters
    public Boolean getPostCommentAdd() {
        return this.postCommentAdd;
    }

    public Boolean getPostCommentDelete() {
        return this.postCommentDelete;
    }

    public Boolean getPostCommentEdit() {
        return this.postCommentEdit;
    }

    // User permissions getters
    public Boolean getUserAdd() {
        return this.userAdd;
    }

    public Boolean getUserDelete() {
        return this.userDelete;
    }

    public Boolean getUserEdit() {
        return this.userEdit;
    }

    // User group permissions getters
    public Boolean getUserGroupAdd() {
        return this.userGroupAdd;
    }

    public Boolean getUserGroupDelete() {
        return this.userGroupDelete;
    }

    public Boolean getUserGroupEdit() {
        return this.userGroupEdit;
    }

    // Account permissions getters
    public Boolean getUserAccountAdd() {
        return this.userAccountAdd;
    }

    public Boolean getUserAccountDelete() {
        return this.userAccountDelete;
    }

    public Boolean getUserAccountEdit() {
        return this.userAccountEdit;
    }

    // User rank permissions getters
    public Boolean getUserRankAdd() {
        return this.userRankAdd;
    }

    public Boolean getUserRankDelete() {
        return this.userRankDelete;
    }

    public Boolean getUserRankEdit() {
        return this.userRankEdit;
    }

    // Setter methods
    public void setId(final Long id) {
        this.id = id;
    }

    public void setName(final String name) {
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

    /** Returns whether any permission is granted. */
    public boolean hasAnyPermission() {
        return this.canAdminView() || this.canAddPage() || this.canDeletePage() || this.canEditPage()
                || this.canAddPost() || this.canDeletePost() || this.canEditPost() || this.canAddPostCategory()
                || this.canDeletePostCategory() || this.canEditPostCategory() || this.canAddPostComment()
                || this.canDeletePostComment() || this.canEditPostComment() || this.canAddUser() || this.canDeleteUser()
                || this.canEditUser() || this.canAddUserGroup() || this.canDeleteUserGroup() || this.canEditUserGroup()
                || this.canAddUserAccount() || this.canDeleteUserAccount() || this.canEditUserAccount()
                || this.canAddUserRank() || this.canDeleteUserRank() || this.canEditUserRank();
    }

}
