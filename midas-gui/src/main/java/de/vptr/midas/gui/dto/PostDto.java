package de.vptr.midas.gui.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.annotation.Nullable;

/** Data transfer object for post. */
public class PostDto extends AbstractTitleContentDto {

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

    @SuppressWarnings({ "NullAway", "PMD.UnnecessaryConstructor" })
    public PostDto() {
        // Fields are filled by from(), the form Binder or a service
    }

    /** Creates the editable form of a post the backend returned. */
    public static PostDto from(final PostResponseDto response) {
        final var dto = new PostDto();
        dto.setPublicId(response.getPublicId());
        dto.setTitle(response.getTitle());
        dto.setContent(response.getContent());
        dto.setUsername(response.getUsername());
        dto.setCategoryPublicId(response.getCategoryPublicId());
        dto.setCategoryName(response.getCategoryName());
        dto.setPublished(response.getPublished());
        dto.setCommentable(response.getCommentable());
        dto.setCreated(response.getCreated());
        dto.setLastEdit(response.getLastEdit());
        dto.setCommentsCount(response.getCommentsCount());
        return dto;
    }

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
