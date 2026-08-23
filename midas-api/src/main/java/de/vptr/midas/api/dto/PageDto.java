package de.vptr.midas.api.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Data transfer object for page data. */
public class PageDto {
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    @Nullable
    public String title;

    @NotBlank(message = "Content is required")
    @Size(max = 50_000, message = "Content must not exceed 50_000 characters")
    @Nullable
    public String content;
}
