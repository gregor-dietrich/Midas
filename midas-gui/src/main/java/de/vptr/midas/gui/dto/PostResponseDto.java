package de.vptr.midas.gui.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for post operations. Contains computed fields and safe data for client responses.
 */
public class PostResponseDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("content")
    private String content;

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

    public PostResponseDto() {
        // Default constructor for Jackson
    }

    public Long getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public String getContent() {
        return this.content;
    }

    public Long getUserId() {
        return this.userId;
    }

    public String getUsername() {
        return this.username;
    }

    public Long getCategoryId() {
        return this.categoryId;
    }

    public String getCategoryName() {
        return this.categoryName;
    }

    public Boolean getPublished() {
        return this.published;
    }

    public Boolean getCommentable() {
        return this.commentable;
    }

    public LocalDateTime getCreated() {
        return this.created;
    }

    public LocalDateTime getLastEdit() {
        return this.lastEdit;
    }

    public Long getCommentsCount() {
        return this.commentsCount;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setCategoryId(final Long categoryId) {
        this.categoryId = categoryId;
    }

    public void setCategoryName(final String categoryName) {
        this.categoryName = categoryName;
    }

    public void setPublished(final Boolean published) {
        this.published = published;
    }

    public void setCommentable(final Boolean commentable) {
        this.commentable = commentable;
    }

    public void setCreated(final LocalDateTime created) {
        this.created = created;
    }

    public void setLastEdit(final LocalDateTime lastEdit) {
        this.lastEdit = lastEdit;
    }

    public void setCommentsCount(final Long commentsCount) {
        this.commentsCount = commentsCount;
    }

    /** Handles the to post dto operation. */
    public PostDto toPostDto() {
        final PostDto dto = new PostDto();
        dto.setId(this.id);
        dto.setTitle(this.title);
        dto.setContent(this.content);
        dto.setUserId(this.userId);
        dto.setUsername(this.username);
        dto.setCategoryId(this.categoryId);
        dto.setCategoryName(this.categoryName);
        dto.setPublished(this.published);
        dto.setCommentable(this.commentable);
        dto.setCreated(this.created);
        dto.setLastEdit(this.lastEdit);
        dto.setCommentsCount(this.commentsCount);
        return dto;
    }

}
