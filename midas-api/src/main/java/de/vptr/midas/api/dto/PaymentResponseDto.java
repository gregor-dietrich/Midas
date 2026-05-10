package de.vptr.midas.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import de.vptr.midas.api.entity.PaymentEntity;
import jakarta.annotation.Nullable;

/** Data transfer object for payment response data. */
public class PaymentResponseDto {
    @Nullable
    public Long id;
    @Nullable
    public Long targetAccountId;
    @Nullable
    public String targetAccountName;
    @Nullable
    public Long sourceAccountId;
    @Nullable
    public String sourceAccountName;
    @Nullable
    public Long userId;
    @Nullable
    public String username;
    @Nullable
    public String comment;
    @Nullable
    public LocalDate date;
    @Nullable
    public BigDecimal amount;
    @Nullable
    public LocalDateTime created;
    @Nullable
    public LocalDateTime lastEdit;

    public PaymentResponseDto() {
    }

    /** Creates a new instance. */
    public PaymentResponseDto(final PaymentEntity entity) {
        this.id = entity.id;
        this.targetAccountId = entity.targetAccount != null ? entity.targetAccount.id : null;
        this.targetAccountName = entity.targetAccount != null ? entity.targetAccount.name : null;
        this.sourceAccountId = entity.sourceAccount != null ? entity.sourceAccount.id : null;
        this.sourceAccountName = entity.sourceAccount != null ? entity.sourceAccount.name : null;
        this.userId = entity.user != null ? entity.user.id : null;
        this.username = entity.user != null ? entity.user.username : null;
        this.comment = entity.comment;
        this.date = entity.date;
        this.amount = entity.amount;
        this.created = entity.created;
        this.lastEdit = entity.lastEdit;
    }
}
