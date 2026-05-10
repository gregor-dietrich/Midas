package de.vptr.midas.api.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Data transfer object for user rank data. */
public class UserRankDto {

    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
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

    public UserRankDto() {
    }

    public UserRankDto(@Nullable final String name) {
        this.name = name;
    }
}
