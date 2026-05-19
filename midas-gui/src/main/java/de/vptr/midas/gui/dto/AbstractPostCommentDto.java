package de.vptr.midas.gui.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.annotation.Nullable;

/** Data transfer object for post comment. */
public abstract class AbstractPostCommentDto {

    @SuppressWarnings("PMD.UnnecessaryConstructor")
    public AbstractPostCommentDto() {
        // Default constructor for Jackson deserialization
    }

    @JsonProperty("publicId")
    @Nullable
    private String publicId;

    @JsonProperty("content")
    @Nullable
    private String content;

    @JsonProperty("postPublicId")
    @Nullable
    private String postPublicId;

    @JsonProperty("postTitle")
    @Nullable
    private String postTitle;

    @JsonProperty("userPublicId")
    @Nullable
    private String userPublicId;

    @JsonProperty("username")
    @Nullable
    private String username;

    @JsonProperty("created")
    @Nullable
    private LocalDateTime created;

    @Nullable
    public String getPublicId() {
        return this.publicId;
    }

    public void setPublicId(@Nullable final String publicId) {
        this.publicId = publicId;
    }

    @Nullable
    public String getContent() {
        return this.content;
    }

    public void setContent(@Nullable final String content) {
        this.content = content;
    }

    @Nullable
    public String getPostPublicId() {
        return this.postPublicId;
    }

    public void setPostPublicId(@Nullable final String postPublicId) {
        this.postPublicId = postPublicId;
    }

    @Nullable
    public String getPostTitle() {
        return this.postTitle;
    }

    public void setPostTitle(@Nullable final String postTitle) {
        this.postTitle = postTitle;
    }

    @Nullable
    public String getUserPublicId() {
        return this.userPublicId;
    }

    public void setUserPublicId(@Nullable final String userPublicId) {
        this.userPublicId = userPublicId;
    }

    @Nullable
    public String getUsername() {
        return this.username;
    }

    public void setUsername(@Nullable final String username) {
        this.username = username;
    }

    @Nullable
    public LocalDateTime getCreated() {
        return this.created;
    }

    public void setCreated(@Nullable final LocalDateTime created) {
        this.created = created;
    }

}
