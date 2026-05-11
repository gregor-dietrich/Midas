package de.vptr.midas.gui.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Response DTO for user operations. Contains computed fields and safe data for client responses. Does not expose
 * sensitive fields like password, salt, or activation key.
 */
@RegisterForReflection
public class UserResponseDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("username")
    private String username;

    @JsonProperty("email")
    private String email;

    @JsonProperty("rankId")
    private Long rankId;

    @JsonProperty("rankName")
    private String rankName;

    @JsonProperty("banned")
    private Boolean banned;

    @JsonProperty("activated")
    private Boolean activated;

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

    @JsonProperty("isNegative")
    private boolean isNegative;

    @SuppressWarnings("NullAway")
    public UserResponseDto() {
        // Default constructor for Jackson
    }

    public Long getId() {
        return this.id;
    }

    public String getUsername() {
        return this.username;
    }

    public String getEmail() {
        return this.email;
    }

    public Long getRankId() {
        return this.rankId;
    }

    public String getRankName() {
        return this.rankName;
    }

    public Boolean getBanned() {
        return this.banned;
    }

    public Boolean getActivated() {
        return this.activated;
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

    public void setEmail(final String email) {
        this.email = email;
    }

    public void setRankId(final Long rankId) {
        this.rankId = rankId;
    }

    public void setRankName(final String rankName) {
        this.rankName = rankName;
    }

    public void setBanned(final Boolean banned) {
        this.banned = banned;
    }

    public void setActivated(final Boolean activated) {
        this.activated = activated;
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

    /**
     * Returns whether this user's account association is flagged as negative. Populated when users are returned in
     * account context.
     */
    public boolean isNegative() {
        return this.isNegative;
    }

    /** Sets whether this user's account association is flagged as negative. */
    public void setIsNegative(final boolean isNegative) {
        this.isNegative = isNegative;
    }

    /** Handles the to user dto operation. */
    public UserDto toUserDto() {
        final UserDto dto = new UserDto();
        dto.setId(this.id);
        dto.setUsername(this.username);
        dto.setEmail(this.email);
        dto.setRankId(this.rankId);
        dto.setRankName(this.rankName);
        dto.setBanned(this.banned);
        dto.setActivated(this.activated);
        dto.setLastIp(this.lastIp);
        dto.setCreated(this.created);
        dto.setLastLogin(this.lastLogin);
        dto.setPostsCount(this.postsCount);
        dto.setCommentsCount(this.commentsCount);
        return dto;
    }

}
