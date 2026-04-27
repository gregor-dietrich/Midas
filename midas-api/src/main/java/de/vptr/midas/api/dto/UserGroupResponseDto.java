package de.vptr.midas.api.dto;

import de.vptr.midas.api.entity.UserGroupEntity;

/** Data transfer object for user group response data. */
public class UserGroupResponseDto {
    public Long id;
    public String name;
    public Long userCount;

    public UserGroupResponseDto() {
    }

    /**
     * Creates a new instance with pre-computed user count (avoids lazy-loading
     * collections).
     */
    public UserGroupResponseDto(final UserGroupEntity entity, final long userCount) {
        this.id = entity.id;
        this.name = entity.name;
        this.userCount = userCount;
    }
}
