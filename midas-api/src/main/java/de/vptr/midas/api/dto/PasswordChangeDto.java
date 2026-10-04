package de.vptr.midas.api.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dedicated DTO for password change operations. Separated from UserDto to avoid exposing rank/banned/activated fields
 * during a password change request.
 */
public class PasswordChangeDto {

    @NotBlank(message = "Current password is required")
    @Nullable
    public String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 72, message = "New password must be between 8 and 72 characters")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).*$",
            message = "Password must contain at" + " least one digit, one lowercase, one uppercase letter and one "
                    + "special character (@#$%^&+=!)")
    @Nullable
    public String newPassword;

    public PasswordChangeDto() {
    }

    public PasswordChangeDto(@Nullable final String currentPassword, @Nullable final String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }
}
