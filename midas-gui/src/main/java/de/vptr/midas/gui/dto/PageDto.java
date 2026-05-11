package de.vptr.midas.gui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Data transfer object for page. */
public class PageDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("content")
    private String content;

    @SuppressWarnings("NullAway")
    public PageDto() {
        // Default constructor for Jackson
    }

    /** Creates a new instance with the given parameters. */
    public PageDto(final Long id, final String title, final String content) {
        this.id = id;
        this.title = title;
        this.content = content;
    }

    // CPD-OFF
    // Getters and setters
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
    // CPD-ON

}
