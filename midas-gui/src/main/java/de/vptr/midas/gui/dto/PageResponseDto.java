package de.vptr.midas.gui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for page operations. Contains safe data for client responses.
 */
public class PageResponseDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("content")
    private String content;

    @SuppressWarnings({ "NullAway", "PMD.UnnecessaryConstructor" })
    public PageResponseDto() {
        // Default constructor for Jackson
    }

    public Long getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public String getContent() {
        return this.content;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    /** Handles the to page dto operation. */
    public PageDto toPageDto() {
        final PageDto dto = new PageDto();
        dto.setId(this.id);
        dto.setTitle(this.title);
        dto.setContent(this.content);
        return dto;
    }

}
