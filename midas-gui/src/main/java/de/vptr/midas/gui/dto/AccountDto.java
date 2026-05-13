package de.vptr.midas.gui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Data transfer object for account. */
public class AccountDto {

    @JsonProperty("publicId")
    private String publicId;

    @JsonProperty("name")
    private String name;

    @SuppressWarnings("NullAway")
    public AccountDto() {
        // Default constructor for Jackson
    }

    @SuppressWarnings("NullAway")
    public AccountDto(final String name) {
        this.name = name;
    }

    public String getPublicId() {
        return this.publicId;
    }

    public String getName() {
        return this.name;
    }

    public void setPublicId(final String publicId) {
        this.publicId = publicId;
    }

    public void setName(final String name) {
        this.name = name;
    }

}
