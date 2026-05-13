package de.vptr.midas.api.dto;

import java.time.LocalDateTime;

import de.vptr.midas.api.entity.UserGroupEntity;
import jakarta.annotation.Nullable;

/** Data transfer object for user group response data. */
public class UserGroupResponseDto {
    @Nullable
    public String publicId;
    @Nullable
    public String name;
    @Nullable
    public Long userCount;
    @Nullable
    public LocalDateTime created;
    @Nullable
    public LocalDateTime lastEdit;

    public UserGroupResponseDto() {
    }

    /**
     * Creates a new instance with pre-computed user count (avoids lazy-loading collections).
     */
    public UserGroupResponseDto(final UserGroupEntity entity, final long userCount) {
        this.publicId = entity.publicId;
        this.name = entity.name;
        this.userCount = userCount;
        this.created = entity.created;
        this.lastEdit = entity.lastEdit;
    }
}
