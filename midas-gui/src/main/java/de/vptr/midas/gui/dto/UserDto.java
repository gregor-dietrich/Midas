package de.vptr.midas.gui.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.annotation.Nullable;

/** Data transfer object for user. */
public class UserDto {

    @JsonProperty("publicId")
    @Nullable
    private String publicId;

    @JsonProperty("username")
    @Nullable
    private String username;

    @JsonProperty("password")
    @Nullable
    private String password;

    @JsonProperty("rankPublicId")
    @Nullable
    private String rankPublicId;

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
    public String getPublicId() {
        return this.publicId;
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
    public String getRankPublicId() {
        return this.rankPublicId;
    }

    @Nullable
    public String getRankName() {
        return this.rankName;
    }

    @Nullable
    public String getEmail() {
        return this.email;
    }

    public boolean isBanned() {
        return Boolean.TRUE.equals(this.banned);
    }

    public boolean isActivated() {
        return Boolean.TRUE.equals(this.activated);
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

    public void setPublicId(@Nullable final String publicId) {
        this.publicId = publicId;
    }

    public void setUsername(@Nullable final String username) {
        this.username = username;
    }

    public void setPassword(@Nullable final String password) {
        this.password = password;
    }

    public void setRankPublicId(@Nullable final String rankPublicId) {
        this.rankPublicId = rankPublicId;
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

    /** Creates the editable form of a user the backend returned; the contract carries no {@code lastIp}. */
    public static UserDto from(final UserResponseDto response) {
        final var dto = new UserDto();
        dto.setPublicId(response.getPublicId());
        dto.setUsername(response.getUsername());
        dto.setEmail(response.getEmail());
        dto.setRankPublicId(response.getRankPublicId());
        dto.setRankName(response.getRankName());
        dto.setBanned(response.getBanned());
        dto.setActivated(response.getActivated());
        dto.setCreated(response.getCreated());
        dto.setLastLogin(response.getLastLogin());
        dto.setPostsCount(response.getPostsCount());
        dto.setCommentsCount(response.getCommentsCount());
        return dto;
    }
}
