package de.vptr.midas.gui.dto;

import jakarta.annotation.Nullable;

/**
 * DTO for password change operations in the GUI.
 */
public class PasswordChangeDto {

    @Nullable
    private String currentPassword;

    @Nullable
    private String newPassword;

    public PasswordChangeDto() {
    }

    public PasswordChangeDto(@Nullable final String currentPassword, @Nullable final String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }

    @Nullable
    public String getCurrentPassword() {
        return this.currentPassword;
    }

    public void setCurrentPassword(@Nullable final String currentPassword) {
        this.currentPassword = currentPassword;
    }

    @Nullable
    public String getNewPassword() {
        return this.newPassword;
    }

    public void setNewPassword(@Nullable final String newPassword) {
        this.newPassword = newPassword;
    }
}
