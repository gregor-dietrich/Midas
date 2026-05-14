package de.vptr.midas.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import de.vptr.midas.api.entity.AccountEntity;
import jakarta.annotation.Nullable;

/**
 * Response DTO for account operations. Contains computed fields and safe data for client responses.
 */
public class AccountResponseDto {

    @Nullable
    public String publicId;
    @Nullable
    public String name;
    @Nullable
    public Long userCount;
    @Nullable
    public List<String> associatedUserPublicIds;
    @Nullable
    public BigDecimal balance;
    public boolean isNegative;
    public boolean canManage;
    @Nullable
    public LocalDateTime created;
    @Nullable
    public LocalDateTime lastEdit;

    public AccountResponseDto() {
    }

    /**
     * Creates a new instance with pre-computed user data (avoids lazy-loading collections).
     */
    public AccountResponseDto(final AccountEntity entity, final int userCount,
            @Nullable final List<String> associatedUserPublicIds) {
        this.publicId = entity.publicId;
        this.name = entity.name;
        this.userCount = (long) userCount;
        this.associatedUserPublicIds = associatedUserPublicIds != null ? associatedUserPublicIds : List.of();
        this.balance = null;
        this.created = entity.created;
        this.lastEdit = entity.lastEdit;
    }

    /** Creates a new instance with pre-computed user data and balance. */
    public AccountResponseDto(final AccountEntity entity, final int userCount,
            @Nullable final List<String> associatedUserPublicIds, @Nullable final BigDecimal balance) {
        this(entity, userCount, associatedUserPublicIds);
        this.balance = balance;
    }
}
