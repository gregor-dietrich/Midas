package de.vptr.midas.api.dto;

import java.time.LocalDateTime;

import de.vptr.midas.api.entity.UserEntity;

/** Data transfer object for user response data. */
public class UserResponseDto {

    public Long id;
    public String username;
    // Note: password and salt are never exposed in responses for security
    public String email;
    public Long rankId;
    public String rankName;
    public Boolean banned;
    public Boolean activated;
    // activationKey is sensitive and not exposed in normal responses
    public LocalDateTime created;
    public LocalDateTime lastLogin;
    public Long postsCount;
    public Long commentsCount;
    // Populated when user is returned in account context (per-account association flag)
    public boolean isNegative;

    public UserResponseDto() {
    }

    /**
     * Creates a new instance with pre-computed counts (avoids lazy-loading
     * collections).
     */
    public UserResponseDto(final UserEntity entity, final long postsCount, final long commentsCount) {
        if (entity != null) {
            this.id = entity.id;
            this.username = entity.username;
            // password and salt are NEVER exposed
            this.email = entity.email;
            this.rankId = entity.rank != null ? entity.rank.id : null;
            this.rankName = entity.rank != null ? entity.rank.name : null;
            this.banned = entity.banned;
            this.activated = entity.activated;
            // activationKey is not exposed for security
            // lastIp is PII and not exposed in responses
            this.created = entity.created;
            this.lastLogin = entity.lastLogin;
            this.postsCount = postsCount;
            this.commentsCount = commentsCount;
        }
    }
}
