package de.vptr.midas.gui.dto;

/** Data transfer object for user rank. */
@SuppressWarnings("PMD.ExcessivePublicCount")
public class UserRankDto extends AbstractUserRankDto {

    @SuppressWarnings("NullAway")
    public UserRankDto() {
        // Default constructor for Jackson
    }

    @SuppressWarnings("NullAway")
    public UserRankDto(final String name) {
        this.setName(name);
    }
}
