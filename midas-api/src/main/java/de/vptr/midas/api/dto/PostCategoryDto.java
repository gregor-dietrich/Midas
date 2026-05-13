package de.vptr.midas.api.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for post category operations (POST, PUT, PATCH).
 *
 * - POST: name required (validated by service), parentId optional - PUT: name required (validated by service), parentId
 * optional for parent changes - PATCH: name optional (allows null), parentId optional for parent changes
 */
public class PostCategoryDto {

    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 255, message = "Name must be between 1 and 255 characters")
    @Nullable
    public String name;

    // Optional parentPublicId for setting parent category
    // null = make it a root category
    // positive value = set to that parent
    @Nullable
    public String parentPublicId;

}
