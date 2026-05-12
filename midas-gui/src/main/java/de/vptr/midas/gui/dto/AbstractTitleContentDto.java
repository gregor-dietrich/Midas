package de.vptr.midas.gui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Base DTO for entities containing id, title, and content fields. */
public abstract class AbstractTitleContentDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("content")
    private String content;

    @SuppressWarnings("NullAway")
    protected AbstractTitleContentDto() {
        // Default constructor
    }

    protected AbstractTitleContentDto(final Long id, final String title, final String content) {
        this.id = id;
        this.title = title;
        this.content = content;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(final String content) {
        this.content = content;
    }
}
