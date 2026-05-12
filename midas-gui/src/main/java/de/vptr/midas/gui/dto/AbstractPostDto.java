package de.vptr.midas.gui.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Data transfer object for post. */
public abstract class AbstractPostDto extends AbstractTitleContentDto {

    @SuppressWarnings("NullAway")
    protected AbstractPostDto() {
        super();
    }

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("categoryId")
    private Long categoryId;

    @JsonProperty("categoryName")
    private String categoryName;

    @JsonProperty("published")
    private Boolean published;

    @JsonProperty("commentable")
    private Boolean commentable;

    @JsonProperty("created")
    private LocalDateTime created;

    @JsonProperty("lastEdit")
    private LocalDateTime lastEdit;

    @JsonProperty("commentsCount")
    private Long commentsCount;

    public Long getUserId() {
        return this.userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public Long getCategoryId() {
        return this.categoryId;
    }

    public void setCategoryId(final Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return this.categoryName;
    }

    public void setCategoryName(final String categoryName) {
        this.categoryName = categoryName;
    }

    public Boolean isPublished() {
        return this.published;
    }

    public void setPublished(final Boolean published) {
        this.published = published;
    }

    public Boolean isCommentable() {
        return this.commentable;
    }

    public void setCommentable(final Boolean commentable) {
        this.commentable = commentable;
    }

    public LocalDateTime getCreated() {
        return this.created;
    }

    public void setCreated(final LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getLastEdit() {
        return this.lastEdit;
    }

    public void setLastEdit(final LocalDateTime lastEdit) {
        this.lastEdit = lastEdit;
    }

    public Long getCommentsCount() {
        return this.commentsCount;
    }

    public void setCommentsCount(final Long commentsCount) {
        this.commentsCount = commentsCount;
    }
}
