package de.vptr.midas.gui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Request DTO for updating user-account meta properties such as the isNegative flag. */
public class UserAccountMetaDto {

    @JsonProperty("isNegative")
    private boolean isNegative;

    /** Returns whether the account association is flagged as negative. */
    public boolean isNegative() {
        return this.isNegative;
    }

    /** Sets whether the account association is flagged as negative. */
    public void setIsNegative(final boolean isNegative) {
        this.isNegative = isNegative;
    }
}
