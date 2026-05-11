package de.vptr.midas.api.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Data transfer object for user rank data. */
public class UserRankDto extends BaseUserRankDto {
    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    @Nullable
    public String name;

    public UserRankDto() {
    }

    public UserRankDto(@Nullable final String name) {
        this.name = name;
    }
}
