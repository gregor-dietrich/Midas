package de.vptr.midas.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import de.vptr.midas.api.entity.AccountEntity;

/**
 * Response DTO for account operations.
 * Contains computed fields and safe data for client responses.
 */
public class AccountResponseDto {

    public Long id;
    public String name;
    public Long userCount;
    public List<Long> associatedUserIds;
    public BigDecimal balance;
    public boolean isNegative;
    public LocalDateTime created;
    public LocalDateTime lastEdit;

    public AccountResponseDto() {
    }

    /**
     * Creates a new instance with pre-computed user data (avoids lazy-loading
     * collections).
     */
    public AccountResponseDto(final AccountEntity entity, final int userCount, final List<Long> associatedUserIds) {
        this.id = entity.id;
        this.name = entity.name;
        this.userCount = (long) userCount;
        this.associatedUserIds = associatedUserIds != null ? associatedUserIds : List.of();
        this.balance = null;
        this.created = entity.created;
        this.lastEdit = entity.lastEdit;
    }

    /** Creates a new instance with pre-computed user data and balance. */
    public AccountResponseDto(final AccountEntity entity, final int userCount, final List<Long> associatedUserIds,
            final BigDecimal balance) {
        this(entity, userCount, associatedUserIds);
        this.balance = balance;
    }
}
