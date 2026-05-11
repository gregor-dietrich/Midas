package de.vptr.midas.gui.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/** Data transfer object for user group. */
@SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "DTO used only within GUI; not serialized across trust boundaries")
public class UserGroupDto {

    @JsonProperty("id")
    private Long id;

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
    public UserGroupDto(final Long id, final String name) {
        this.id = id;
        this.name = name;
    }

    // Getters and setters
    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
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
