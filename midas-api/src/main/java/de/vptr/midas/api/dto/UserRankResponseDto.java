package de.vptr.midas.api.dto;

import java.time.LocalDateTime;

import de.vptr.midas.api.entity.UserRankEntity;
import jakarta.annotation.Nullable;

/** Data transfer object for user rank response data. */
public class UserRankResponseDto {

    @Nullable
    public Long id;
    @Nullable
    public String name;

    // View permissions
    @Nullable
    public Boolean adminView;

    // Page permissions
    @Nullable
    public Boolean pageAdd;
    @Nullable
    public Boolean pageDelete;
    @Nullable
    public Boolean pageEdit;

    // Post permissions
    @Nullable
    public Boolean postAdd;
    @Nullable
    public Boolean postDelete;
    @Nullable
    public Boolean postEdit;

    // Post category permissions
    @Nullable
    public Boolean postCategoryAdd;
    @Nullable
    public Boolean postCategoryDelete;
    @Nullable
    public Boolean postCategoryEdit;

    // Post comment permissions
    @Nullable
    public Boolean postCommentAdd;
    @Nullable
    public Boolean postCommentDelete;
    @Nullable
    public Boolean postCommentEdit;

    // User permissions
    @Nullable
    public Boolean userAdd;
    @Nullable
    public Boolean userDelete;
    @Nullable
    public Boolean userEdit;

    // User group permissions
    @Nullable
    public Boolean userGroupAdd;
    @Nullable
    public Boolean userGroupDelete;
    @Nullable
    public Boolean userGroupEdit;

    // Account permissions
    @Nullable
    public Boolean userAccountAdd;
    @Nullable
    public Boolean userAccountDelete;
    @Nullable
    public Boolean userAccountEdit;

    // User rank permissions
    @Nullable
    public Boolean userRankAdd;
    @Nullable
    public Boolean userRankDelete;
    @Nullable
    public Boolean userRankEdit;

    // Computed fields
    @Nullable
    public Long usersCount;
    @Nullable
    public LocalDateTime created;
    @Nullable
    public LocalDateTime lastEdit;

    public UserRankResponseDto() {
    }

    /**
     * Creates a new instance with pre-computed user count (avoids lazy-loading collections).
     */
    public UserRankResponseDto(@Nullable final UserRankEntity entity, final long usersCount) {
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
            this.created = entity.created;
            this.lastEdit = entity.lastEdit;
        }
    }
}
