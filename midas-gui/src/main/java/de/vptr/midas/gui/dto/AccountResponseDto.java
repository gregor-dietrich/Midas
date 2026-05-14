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

    @JsonProperty("publicId")
    private String publicId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("userCount")
    private int userCount;

    @JsonProperty("associatedUserPublicIds")
    private List<String> associatedUserPublicIds;

    @JsonProperty("balance")
    private BigDecimal balance; // Optional balance field

    @JsonProperty("isNegative")
    private boolean isNegative;

    @JsonProperty("canManage")
    private boolean canManage;

    @SuppressWarnings({ "NullAway", "PMD.UnnecessaryConstructor" })
    public AccountResponseDto() {
        // Default constructor for Jackson
    }

    public String getPublicId() {
        return this.publicId;
    }

    public String getName() {
        return this.name;
    }

    public int getUserCount() {
        return this.userCount;
    }

    public List<String> getAssociatedUserPublicIds() {
        return this.associatedUserPublicIds;
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

    /** Returns whether the user can manage payments for this account. */
    public boolean isCanManage() {
        return this.canManage;
    }

    /** Sets whether the user can manage payments for this account. */
    public void setCanManage(final boolean canManage) {
        this.canManage = canManage;
    }

    public void setPublicId(final String publicId) {
        this.publicId = publicId;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setUserCount(final int userCount) {
        this.userCount = userCount;
    }

    public void setAssociatedUserPublicIds(final List<String> associatedUserPublicIds) {
        this.associatedUserPublicIds = associatedUserPublicIds;
    }

    /** Handles the to account dto operation. */
    public AccountDto toAccountDto() {
        final AccountDto dto = new AccountDto();
        dto.setPublicId(this.publicId);
        dto.setName(this.name);
        return dto;
    }

}
