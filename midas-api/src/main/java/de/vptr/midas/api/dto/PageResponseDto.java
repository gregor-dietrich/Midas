package de.vptr.midas.api.dto;

import java.time.LocalDateTime;

import de.vptr.midas.api.entity.PageEntity;
import jakarta.annotation.Nullable;

/** Data transfer object for page response data. */
public class PageResponseDto {
    @Nullable
    public Long id;
    @Nullable
    public String title;
    @Nullable
    public String content;
    @Nullable
    public LocalDateTime created;
    @Nullable
    public LocalDateTime lastEdit;

    public PageResponseDto() {
    }

    /** Creates a new instance. */
    public PageResponseDto(final PageEntity entity) {
        this.id = entity.id;
        this.title = entity.title;
        this.content = entity.content;
        this.created = entity.created;
        this.lastEdit = entity.lastEdit;
    }
}
