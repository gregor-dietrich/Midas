package de.vptr.midas.gui.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Data transfer object for post comment. */
public class PostCommentDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("content")
    private String content;

    @JsonProperty("postId")
    private Long postId;

    @JsonProperty("postTitle")
    private String postTitle;

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("created")
    private LocalDateTime created;

    public PostCommentDto() {
        // Default constructor for Jackson
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public Long getPostId() {
        return this.postId;
    }

    public void setPostId(final Long postId) {
        this.postId = postId;
    }

    public String getPostTitle() {
        return this.postTitle;
    }

    public void setPostTitle(final String postTitle) {
        this.postTitle = postTitle;
    }

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

    public LocalDateTime getCreated() {
        return this.created;
    }

    public void setCreated(final LocalDateTime created) {
        this.created = created;
    }

}
