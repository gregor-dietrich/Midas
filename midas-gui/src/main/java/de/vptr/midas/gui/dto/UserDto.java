package de.vptr.midas.gui.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.annotation.Nullable;

/** Data transfer object for user. */
public class UserDto {

    @JsonProperty("id")
    @Nullable
    private Long id;

    @JsonProperty("username")
    @Nullable
    private String username;

    @JsonProperty("password")
    @Nullable
    private String password;

    @JsonProperty("rankId")
    @Nullable
    private Long rankId;

    @JsonProperty("rankName")
    @Nullable
    private String rankName;

    @JsonProperty("email")
    @Nullable
    private String email;

    @JsonProperty("banned")
    @Nullable
    private Boolean banned;

    @JsonProperty("activated")
    @Nullable
    private Boolean activated;

    @JsonProperty("activationKey")
    @Nullable
    private String activationKey;

    @JsonProperty("lastIp")
    @Nullable
    private String lastIp;

    @JsonProperty("created")
    @Nullable
    private LocalDateTime created;

    @JsonProperty("lastLogin")
    @Nullable
    private LocalDateTime lastLogin;

    @JsonProperty("postsCount")
    @Nullable
    private Long postsCount;

    @JsonProperty("commentsCount")
    @Nullable
    private Long commentsCount;

    public UserDto() {
        // Default constructor for Jackson
    }

    public UserDto(@Nullable final String username, @Nullable final String email) {
        this.username = username;
        this.email = email;
    }

    @Nullable
    public Long getId() {
        return this.id;
    }

    @Nullable
    public String getUsername() {
        return this.username;
    }

    @Nullable
    public String getPassword() {
        return this.password;
    }

    @Nullable
    public Long getRankId() {
        return this.rankId;
    }

    @Nullable
    public String getRankName() {
        return this.rankName;
    }

    @Nullable
    public String getEmail() {
        return this.email;
    }

    @Nullable
    public Boolean getBanned() {
        return this.banned;
    }

    @Nullable
    public Boolean getActivated() {
        return this.activated;
    }

    @Nullable
    public String getActivationKey() {
        return this.activationKey;
    }

    @Nullable
    public String getLastIp() {
        return this.lastIp;
    }

    @Nullable
    public LocalDateTime getCreated() {
        return this.created;
    }

    @Nullable
    public LocalDateTime getLastLogin() {
        return this.lastLogin;
    }

    @Nullable
    public Long getPostsCount() {
        return this.postsCount;
    }

    @Nullable
    public Long getCommentsCount() {
        return this.commentsCount;
    }

    public void setId(@Nullable final Long id) {
        this.id = id;
    }

    public void setUsername(@Nullable final String username) {
        this.username = username;
    }

    public void setPassword(@Nullable final String password) {
        this.password = password;
    }

    public void setRankId(@Nullable final Long rankId) {
        this.rankId = rankId;
    }

    public void setRankName(@Nullable final String rankName) {
        this.rankName = rankName;
    }

    public void setEmail(@Nullable final String email) {
        this.email = email;
    }

    public void setBanned(@Nullable final Boolean banned) {
        this.banned = banned;
    }

    public void setActivated(@Nullable final Boolean activated) {
        this.activated = activated;
    }

    public void setActivationKey(@Nullable final String activationKey) {
        this.activationKey = activationKey;
    }

    public void setLastIp(@Nullable final String lastIp) {
        this.lastIp = lastIp;
    }

    public void setCreated(@Nullable final LocalDateTime created) {
        this.created = created;
    }

    public void setLastLogin(@Nullable final LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public void setPostsCount(@Nullable final Long postsCount) {
        this.postsCount = postsCount;
    }

    public void setCommentsCount(@Nullable final Long commentsCount) {
        this.commentsCount = commentsCount;
    }

    public boolean isActivated() {
        return this.activated != null && this.activated;
    }

    public boolean isBanned() {
        return this.banned != null && this.banned;
    }
}
