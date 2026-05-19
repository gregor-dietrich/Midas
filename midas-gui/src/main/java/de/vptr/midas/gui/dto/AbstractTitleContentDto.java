package de.vptr.midas.gui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.annotation.Nullable;

/** Base DTO for entities containing publicId, title, and content fields. */
public abstract class AbstractTitleContentDto {

    @JsonProperty("publicId")
    @Nullable
    private String publicId;

    @JsonProperty("title")
    @Nullable
    private String title;

    @JsonProperty("content")
    @Nullable
    private String content;

    protected AbstractTitleContentDto() {
        // Default constructor
    }

    protected AbstractTitleContentDto(final String publicId, final String title, final String content) {
        this.publicId = publicId;
        this.title = title;
        this.content = content;
    }

    @Nullable
    public String getPublicId() {
        return this.publicId;
    }

    public void setPublicId(@Nullable final String publicId) {
        this.publicId = publicId;
    }

    @Nullable
    public String getTitle() {
        return this.title;
    }

    public void setTitle(@Nullable final String title) {
        this.title = title;
    }

    @Nullable
    public String getContent() {
        return this.content;
    }

    public void setContent(@Nullable final String content) {
        this.content = content;
    }
}
