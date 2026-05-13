package de.vptr.midas.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import de.vptr.midas.api.entity.PaymentEntity;
import jakarta.annotation.Nullable;

/** Data transfer object for payment response data. */
public class PaymentResponseDto {
    @Nullable
    public String publicId;
    @Nullable
    public String targetAccountPublicId;
    @Nullable
    public String targetAccountName;
    @Nullable
    public String sourceAccountPublicId;
    @Nullable
    public String sourceAccountName;
    @Nullable
    public String userPublicId;
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
        this.publicId = entity.publicId;
        this.targetAccountPublicId = entity.targetAccount != null ? entity.targetAccount.publicId : null;
        this.targetAccountName = entity.targetAccount != null ? entity.targetAccount.name : null;
        this.sourceAccountPublicId = entity.sourceAccount != null ? entity.sourceAccount.publicId : null;
        this.sourceAccountName = entity.sourceAccount != null ? entity.sourceAccount.name : null;
        this.userPublicId = entity.user != null ? entity.user.publicId : null;
        this.username = entity.user != null ? entity.user.username : null;
        this.comment = entity.comment;
        this.date = entity.date;
        this.amount = entity.amount;
        this.created = entity.created;
        this.lastEdit = entity.lastEdit;
    }
}
