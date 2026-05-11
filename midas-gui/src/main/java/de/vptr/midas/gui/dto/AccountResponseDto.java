package de.vptr.midas.gui.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Response DTO for account operations. Contains computed fields and safe data for client responses.
 */
@SuppressFBWarnings(value = "EI_EXPOSE_REP",
        justification = "DTO used only within GUI; not serialized across trust boundaries")
public class AccountResponseDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("userCount")
    private int userCount;

    @JsonProperty("associatedUserIds")
    private List<Long> associatedUserIds;

    @JsonProperty("balance")
    private BigDecimal balance; // Optional balance field

    @JsonProperty("isNegative")
    private boolean isNegative;

    @SuppressWarnings({ "NullAway", "PMD.UnnecessaryConstructor" })
    public AccountResponseDto() {
        // Default constructor for Jackson
    }

    public Long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public int getUserCount() {
        return this.userCount;
    }

    public List<Long> getAssociatedUserIds() {
        return this.associatedUserIds;
    }

    public BigDecimal getBalance() {
        return this.balance;
    }

    public void setBalance(final BigDecimal balance) {
        this.balance = balance;
    }

    /** Returns whether this account counts negatively toward the user's total. */
    public boolean isNegative() {
        return this.isNegative;
    }

    /** Sets whether this account counts negatively toward the user's total. */
    public void setIsNegative(final boolean isNegative) {
        this.isNegative = isNegative;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setUserCount(final int userCount) {
        this.userCount = userCount;
    }

    public void setAssociatedUserIds(final List<Long> associatedUserIds) {
        this.associatedUserIds = associatedUserIds;
    }

    /** Handles the to account dto operation. */
    public AccountDto toAccountDto() {
        final AccountDto dto = new AccountDto();
        dto.setId(this.id);
        dto.setName(this.name);
        return dto;
    }

}
