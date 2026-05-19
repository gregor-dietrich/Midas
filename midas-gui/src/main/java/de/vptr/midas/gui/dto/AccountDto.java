package de.vptr.midas.gui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.annotation.Nullable;

/** Data transfer object for account. */
public class AccountDto {

    @JsonProperty("publicId")
    @Nullable
    private String publicId;

    @JsonProperty("name")
    @Nullable
    private String name;

    public AccountDto() {
        // Default constructor for Jackson
    }

    public AccountDto(final String name) {
        this.name = name;
    }

    @Nullable
    public String getPublicId() {
        return this.publicId;
    }

    @Nullable
    public String getName() {
        return this.name;
    }

    public void setPublicId(@Nullable final String publicId) {
        this.publicId = publicId;
    }

    public void setName(@Nullable final String name) {
        this.name = name;
    }

}
