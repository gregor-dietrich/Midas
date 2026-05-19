package de.vptr.midas.gui.dto;

/** Data transfer object for user rank. */
public class UserRankDto extends AbstractUserRankDto {

    public UserRankDto() {
        // Default constructor for Jackson
    }

    public UserRankDto(final String name) {
        this.setName(name);
    }
}
