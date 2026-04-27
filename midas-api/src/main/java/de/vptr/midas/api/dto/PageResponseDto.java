package de.vptr.midas.api.dto;

import java.time.LocalDateTime;

import de.vptr.midas.api.entity.PageEntity;

/** Data transfer object for page response data. */
public class PageResponseDto {
    public Long id;
    public String title;
    public String content;
    public LocalDateTime created;
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
