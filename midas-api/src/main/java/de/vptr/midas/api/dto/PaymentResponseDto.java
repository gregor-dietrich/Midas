package de.vptr.midas.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import de.vptr.midas.api.entity.PaymentEntity;

/** Data transfer object for payment response data. */
public class PaymentResponseDto {
    public Long id;
    public Long targetAccountId;
    public String targetAccountName;
    public Long sourceAccountId;
    public String sourceAccountName;
    public Long userId;
    public String username;
    public String comment;
    public LocalDate date;
    public BigDecimal amount;
    public LocalDateTime created;
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
        this.userId = entity.userId != null ? entity.userId.id : null;
        this.username = entity.userId != null ? entity.userId.username : null;
        this.comment = entity.comment;
        this.date = entity.date;
        this.amount = entity.amount;
        this.created = entity.created;
        this.lastEdit = entity.lastEdit;
    }
}
