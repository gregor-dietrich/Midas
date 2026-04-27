package de.vptr.midas.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Data transfer object for user group data. */
public class UserGroupDto {
    @NotBlank(message = "Name is required")
    public String name;
}
