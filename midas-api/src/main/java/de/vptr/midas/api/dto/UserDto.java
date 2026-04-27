package de.vptr.midas.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/** Data transfer object for user data. */
public class UserDto {

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    public String username;

    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    public String password;

    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    public String email;

    public Long rankId;

    public Boolean banned;

    public Boolean activated;

    @Size(max = 255, message = "Activation key must not exceed 255 characters")
    public String activationKey;

    @Size(max = 45, message = "IP address must not exceed 45 characters")
    public String lastIp;

    public UserDto() {
    }

    /** Creates a new instance. */
    public UserDto(final String username, final String password, final String email, final Long rankId,
            final Boolean banned, final Boolean activated,
            final String activationKey, final String lastIp) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.rankId = rankId;
        this.banned = banned;
        this.activated = activated;
        this.activationKey = activationKey;
        this.lastIp = lastIp;
    }
}
