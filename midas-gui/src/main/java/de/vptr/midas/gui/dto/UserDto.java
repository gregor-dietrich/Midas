package de.vptr.midas.gui.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Data transfer object for user. */
public class UserDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private String password;

    @JsonProperty("rankId")
    private Long rankId;

    @JsonProperty("rankName")
    private String rankName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("banned")
    private Boolean banned;

    @JsonProperty("activated")
    private Boolean activated;

    @JsonProperty("activationKey")
    private String activationKey;

    @JsonProperty("lastIp")
    private String lastIp;

    @JsonProperty("created")
    private LocalDateTime created;

    @JsonProperty("lastLogin")
    private LocalDateTime lastLogin;

    @JsonProperty("postsCount")
    private Long postsCount;

    @JsonProperty("commentsCount")
    private Long commentsCount;

    public UserDto() {
        // Default constructor for Jackson
    }

    public UserDto(final String username, final String email) {
        this.username = username;
        this.email = email;
    }

    public Long getId() {
        return this.id;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public Long getRankId() {
        return this.rankId;
    }

    public String getRankName() {
        return this.rankName;
    }

    public String getEmail() {
        return this.email;
    }

    public Boolean getBanned() {
        return this.banned;
    }

    public Boolean getActivated() {
        return this.activated;
    }

    public String getActivationKey() {
        return this.activationKey;
    }

    public String getLastIp() {
        return this.lastIp;
    }

    public LocalDateTime getCreated() {
        return this.created;
    }

    public LocalDateTime getLastLogin() {
        return this.lastLogin;
    }

    public Long getPostsCount() {
        return this.postsCount;
    }

    public Long getCommentsCount() {
        return this.commentsCount;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public void setRankId(final Long rankId) {
        this.rankId = rankId;
    }

    public void setRankName(final String rankName) {
        this.rankName = rankName;
    }

    public void setEmail(final String email) {
        this.email = email;
    }

    public void setBanned(final Boolean banned) {
        this.banned = banned;
    }

    public void setActivated(final Boolean activated) {
        this.activated = activated;
    }

    public void setActivationKey(final String activationKey) {
        this.activationKey = activationKey;
    }

    public void setLastIp(final String lastIp) {
        this.lastIp = lastIp;
    }

    public void setCreated(final LocalDateTime created) {
        this.created = created;
    }

    public void setLastLogin(final LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public void setPostsCount(final Long postsCount) {
        this.postsCount = postsCount;
    }

    public void setCommentsCount(final Long commentsCount) {
        this.commentsCount = commentsCount;
    }

    public boolean isActivated() {
        return this.activated != null && this.activated;
    }

    public boolean isBanned() {
        return this.banned != null && this.banned;
    }

}
