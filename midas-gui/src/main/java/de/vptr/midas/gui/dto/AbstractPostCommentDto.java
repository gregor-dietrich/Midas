package de.vptr.midas.gui.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Data transfer object for post comment. */
public abstract class AbstractPostCommentDto {

    // JUSTIFICATION: Constructor is required by NullAway to initialize fields; cannot be omitted.
    @SuppressWarnings({ "NullAway", "PMD.UnnecessaryConstructor" })
    public AbstractPostCommentDto() {
        // Default constructor for Jackson deserialization and NullAway compliance
    }

    @JsonProperty("publicId")
    private String publicId;

    @JsonProperty("content")
    private String content;

    @JsonProperty("postPublicId")
    private String postPublicId;

    @JsonProperty("postTitle")
    private String postTitle;

    @JsonProperty("userPublicId")
    private String userPublicId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("created")
    private LocalDateTime created;

    public String getPublicId() {
        return this.publicId;
    }

    public void setPublicId(final String publicId) {
        this.publicId = publicId;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public String getPostPublicId() {
        return this.postPublicId;
    }

    public void setPostPublicId(final String postPublicId) {
        this.postPublicId = postPublicId;
    }

    public String getPostTitle() {
        return this.postTitle;
    }

    public void setPostTitle(final String postTitle) {
        this.postTitle = postTitle;
    }

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

    public LocalDateTime getCreated() {
        return this.created;
    }

    public void setCreated(final LocalDateTime created) {
        this.created = created;
    }

}
