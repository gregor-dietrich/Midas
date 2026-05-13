package de.vptr.midas.gui.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.Nullable;

/** Data transfer object for user group. */
@SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "DTO used only within GUI; not serialized across trust boundaries")
public class UserGroupDto {

    @JsonProperty("publicId")
    @Nullable
    private String publicId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("userCount")
    private Long userCount;

    @JsonProperty("users")
    private List<UserDto> users;

    @JsonProperty("created")
    private LocalDateTime created;

    @SuppressWarnings("NullAway")
    public UserGroupDto() {
        // Default constructor for Jackson
    }

    @SuppressWarnings("NullAway")
    public UserGroupDto(@Nullable final String publicId, final String name) {
        this.publicId = publicId;
        this.name = name;
    }

    // Getters and setters
    @Nullable
    public String getPublicId() {
        return this.publicId;
    }

    public void setPublicId(@Nullable final String publicId) {
        this.publicId = publicId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Long getUserCount() {
        return this.userCount;
    }

    public void setUserCount(final Long userCount) {
        this.userCount = userCount;
    }

    public List<UserDto> getUsers() {
        return this.users;
    }

    public void setUsers(final List<UserDto> users) {
        this.users = users;
    }

    public LocalDateTime getCreated() {
        return this.created;
    }

    public void setCreated(final LocalDateTime created) {
        this.created = created;
    }

}
