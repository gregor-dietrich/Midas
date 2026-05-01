package de.vptr.midas.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dedicated DTO for password change operations.
 * Separated from UserDto to avoid exposing rank/banned/activated fields
 * during a password change request.
 */
public class PasswordChangeDto {

    @NotBlank(message = "Current password is required")
    public String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 100, message = "New password must be between 6 and 100 characters")
    public String newPassword;

    public PasswordChangeDto() {
    }

    public PasswordChangeDto(final String currentPassword, final String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }
}
