package de.vptr.midas.gui.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.annotation.Nullable;

/** Data transfer object for post. */
public abstract class AbstractPostDto extends AbstractTitleContentDto {

    protected AbstractPostDto() {
        super();
    }

    @JsonProperty("username")
    @Nullable
    private String username;

    @JsonProperty("categoryPublicId")
    @Nullable
    private String categoryPublicId;

    @JsonProperty("categoryName")
    @Nullable
    private String categoryName;

    @JsonProperty("published")
    @Nullable
    private Boolean published;

    @JsonProperty("commentable")
    @Nullable
    private Boolean commentable;

    @JsonProperty("created")
    @Nullable
    private LocalDateTime created;

    @JsonProperty("lastEdit")
    @Nullable
    private LocalDateTime lastEdit;

    @JsonProperty("commentsCount")
    @Nullable
    private Long commentsCount;

    @Nullable
    public String getUsername() {
        return this.username;
    }

    public void setUsername(@Nullable final String username) {
        this.username = username;
    }

    @Nullable
    public String getCategoryPublicId() {
        return this.categoryPublicId;
    }

    public void setCategoryPublicId(@Nullable final String categoryPublicId) {
        this.categoryPublicId = categoryPublicId;
    }

    @Nullable
    public String getCategoryName() {
        return this.categoryName;
    }

    public void setCategoryName(@Nullable final String categoryName) {
        this.categoryName = categoryName;
    }

    @Nullable
    public Boolean isPublished() {
        return this.published;
    }

    public void setPublished(@Nullable final Boolean published) {
        this.published = published;
    }

    @Nullable
    public Boolean isCommentable() {
        return this.commentable;
    }

    public void setCommentable(@Nullable final Boolean commentable) {
        this.commentable = commentable;
    }

    @Nullable
    public LocalDateTime getCreated() {
        return this.created;
    }

    public void setCreated(@Nullable final LocalDateTime created) {
        this.created = created;
    }

    @Nullable
    public LocalDateTime getLastEdit() {
        return this.lastEdit;
    }

    public void setLastEdit(@Nullable final LocalDateTime lastEdit) {
        this.lastEdit = lastEdit;
    }

    @Nullable
    public Long getCommentsCount() {
        return this.commentsCount;
    }

    public void setCommentsCount(@Nullable final Long commentsCount) {
        this.commentsCount = commentsCount;
    }
}
