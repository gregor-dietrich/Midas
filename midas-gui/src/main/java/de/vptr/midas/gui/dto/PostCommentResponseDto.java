package de.vptr.midas.gui.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for post comment operations. Contains computed fields and safe data for client responses.
 */
public class PostCommentResponseDto {

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

    @SuppressWarnings({ "NullAway", "PMD.UnnecessaryConstructor" })
    public PostCommentResponseDto() {
        // Default constructor for Jackson
    }

    public Long getId() {
        return this.id;
    }

    public String getContent() {
        return this.content;
    }

    public Long getPostId() {
        return this.postId;
    }

    public String getPostTitle() {
        return this.postTitle;
    }

    public Long getUserId() {
        return this.userId;
    }

    public String getUsername() {
        return this.username;
    }

    public LocalDateTime getCreated() {
        return this.created;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public void setPostId(final Long postId) {
        this.postId = postId;
    }

    public void setPostTitle(final String postTitle) {
        this.postTitle = postTitle;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setCreated(final LocalDateTime created) {
        this.created = created;
    }

    /** Handles the to post comment dto operation. */
    public PostCommentDto toPostCommentDto() {
        final PostCommentDto dto = new PostCommentDto();
        dto.setId(this.id);
        dto.setContent(this.content);
        dto.setPostId(this.postId);
        dto.setPostTitle(this.postTitle);
        dto.setUserId(this.userId);
        dto.setUsername(this.username);
        dto.setCreated(this.created);
        return dto;
    }

}
