package de.vptr.midas.gui.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Data transfer object for post. */
public abstract class AbstractPostDto extends AbstractTitleContentDto {

    @SuppressWarnings("NullAway")
    protected AbstractPostDto() {
        super();
    }

    @JsonProperty("userPublicId")
    private String userPublicId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("categoryPublicId")
    private String categoryPublicId;

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

    public String getUserPublicId() {
        return this.userPublicId;
    }

    public void setUserPublicId(final String userPublicId) {
        this.userPublicId = userPublicId;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getCategoryPublicId() {
        return this.categoryPublicId;
    }

    public void setCategoryPublicId(final String categoryPublicId) {
        this.categoryPublicId = categoryPublicId;
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
