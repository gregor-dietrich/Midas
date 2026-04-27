package de.vptr.midas.api.dto;

import de.vptr.midas.api.entity.UserRankEntity;

/** Data transfer object for user rank response data. */
public class UserRankResponseDto {

    public Long id;
    public String name;

    // View permissions
    public Boolean adminView;

    // Page permissions
    public Boolean pageAdd;
    public Boolean pageDelete;
    public Boolean pageEdit;

    // Post permissions
    public Boolean postAdd;
    public Boolean postDelete;
    public Boolean postEdit;

    // Post category permissions
    public Boolean postCategoryAdd;
    public Boolean postCategoryDelete;
    public Boolean postCategoryEdit;

    // Post comment permissions
    public Boolean postCommentAdd;
    public Boolean postCommentDelete;
    public Boolean postCommentEdit;

    // User permissions
    public Boolean userAdd;
    public Boolean userDelete;
    public Boolean userEdit;

    // User group permissions
    public Boolean userGroupAdd;
    public Boolean userGroupDelete;
    public Boolean userGroupEdit;

    // Account permissions
    public Boolean userAccountAdd;
    public Boolean userAccountDelete;
    public Boolean userAccountEdit;

    // User rank permissions
    public Boolean userRankAdd;
    public Boolean userRankDelete;
    public Boolean userRankEdit;

    // Computed fields
    public Long usersCount;

    public UserRankResponseDto() {
    }

    /**
     * Creates a new instance with pre-computed user count (avoids lazy-loading
     * collections).
     */
    public UserRankResponseDto(final UserRankEntity entity, final long usersCount) {
        if (entity != null) {
            this.id = entity.id;
            this.name = entity.name;

            this.adminView = entity.adminView;
            this.pageAdd = entity.pageAdd;
            this.pageDelete = entity.pageDelete;
            this.pageEdit = entity.pageEdit;
            this.postAdd = entity.postAdd;
            this.postDelete = entity.postDelete;
            this.postEdit = entity.postEdit;
            this.postCategoryAdd = entity.postCategoryAdd;
            this.postCategoryDelete = entity.postCategoryDelete;
            this.postCategoryEdit = entity.postCategoryEdit;
            this.postCommentAdd = entity.postCommentAdd;
            this.postCommentDelete = entity.postCommentDelete;
            this.postCommentEdit = entity.postCommentEdit;
            this.userAdd = entity.userAdd;
            this.userDelete = entity.userDelete;
            this.userEdit = entity.userEdit;
            this.userGroupAdd = entity.userGroupAdd;
            this.userGroupDelete = entity.userGroupDelete;
            this.userGroupEdit = entity.userGroupEdit;
            this.userAccountAdd = entity.userAccountAdd;
            this.userAccountDelete = entity.userAccountDelete;
            this.userAccountEdit = entity.userAccountEdit;
            this.userRankAdd = entity.userRankAdd;
            this.userRankDelete = entity.userRankDelete;
            this.userRankEdit = entity.userRankEdit;

            this.usersCount = usersCount;
        }
    }
}
