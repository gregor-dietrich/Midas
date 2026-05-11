package de.vptr.midas.gui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for user group operations. Contains computed fields and safe data for client responses.
 */
public class UserGroupResponseDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("userCount")
    private Long userCount;

    @SuppressWarnings({ "NullAway", "PMD.UnnecessaryConstructor" })
    public UserGroupResponseDto() {
        // Default constructor for Jackson
    }

    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public Long getUserCount() {
        return this.userCount;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setUserCount(final Long userCount) {
        this.userCount = userCount;
    }

    /** Handles the to user group dto operation. */
    public UserGroupDto toUserGroupDto() {
        final UserGroupDto dto = new UserGroupDto();
        dto.setId(this.id);
        dto.setName(this.name);
        return dto;
    }

}
