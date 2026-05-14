package de.vptr.midas.api.dto;

import java.time.LocalDateTime;

import de.vptr.midas.api.entity.UserEntity;
import jakarta.annotation.Nullable;

/** Data transfer object for user response data. */
public class UserResponseDto {

    @Nullable
    public String publicId;
    @Nullable
    public String username;
    // Note: password and salt are never exposed in responses for security
    @Nullable
    public String email;
    @Nullable
    public String rankPublicId;
    @Nullable
    public String rankName;
    @Nullable
    public Boolean banned;
    @Nullable
    public Boolean activated;
    // activationKey is sensitive and not exposed in normal responses
    @Nullable
    public LocalDateTime created;
    @Nullable
    public LocalDateTime lastLogin;
    @Nullable
    public Long postsCount;
    @Nullable
    public Long commentsCount;
    // Populated when user is returned in account context (per-account association flag)
    @Nullable
    public Boolean isNegative;
    @Nullable
    public Boolean canManage;

    public UserResponseDto() {
    }

    /**
     * Creates a new instance with pre-computed counts (avoids lazy-loading collections).
     */
    public UserResponseDto(@Nullable final UserEntity entity, final long postsCount, final long commentsCount) {
        if (entity != null) {
            this.publicId = entity.publicId;
            this.username = entity.username;
            // password and salt are NEVER exposed
            this.email = entity.email;
            this.rankPublicId = entity.rank != null ? entity.rank.publicId : null;
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
