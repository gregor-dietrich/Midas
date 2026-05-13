package de.vptr.midas.api.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Data transfer object for user data. */
public class UserDto {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Nullable
    public String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    @Nullable
    public String password;

    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    @Nullable
    public String email;

    @Nullable
    public String rankPublicId;

    @Nullable
    public Boolean banned;

    @Nullable
    public Boolean activated;

    @Size(max = 255, message = "Activation key must not exceed 255 characters")
    @Nullable
    public String activationKey;

    @Size(max = 45, message = "IP address must not exceed 45 characters")
    @Nullable
    public String lastIp;

    public UserDto() {
    }

    /** Creates a new instance. */
    public UserDto(@Nullable final String username, @Nullable final String password, @Nullable final String email,
            @Nullable final String rankPublicId, @Nullable final Boolean banned, @Nullable final Boolean activated,
            @Nullable final String activationKey, @Nullable final String lastIp) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.rankPublicId = rankPublicId;
        this.banned = banned;
        this.activated = activated;
        this.activationKey = activationKey;
        this.lastIp = lastIp;
    }
}
