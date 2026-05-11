package de.vptr.midas.gui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Data transfer object for account. */
public class AccountDto {

    @JsonProperty("id")
    private Long id;

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

    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setName(final String name) {
        this.name = name;
    }

}
