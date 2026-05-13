package de.vptr.midas.gui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Base DTO for entities containing publicId, title, and content fields. */
public abstract class AbstractTitleContentDto {

    @JsonProperty("publicId")
    private String publicId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("content")
    private String content;

    @SuppressWarnings("NullAway")
    protected AbstractTitleContentDto() {
        // Default constructor
    }

    protected AbstractTitleContentDto(final String publicId, final String title, final String content) {
        this.publicId = publicId;
        this.title = title;
        this.content = content;
    }

    public String getPublicId() {
        return this.publicId;
    }

    public void setPublicId(final String publicId) {
        this.publicId = publicId;
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
