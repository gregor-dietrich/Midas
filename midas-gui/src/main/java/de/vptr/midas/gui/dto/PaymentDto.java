package de.vptr.midas.gui.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.annotation.Nullable;

/** Data transfer object for payment. */
public class PaymentDto {

    @JsonProperty("id")
    @Nullable
    private Long id;

    @JsonProperty("userId")
    @Nullable
    private Long userId;

    @JsonProperty("sourceAccountId")
    @Nullable
    private Long sourceAccountId;

    @JsonProperty("targetAccountId")
    @Nullable
    private Long targetAccountId;

    @JsonProperty("amount")
    @Nullable
    private BigDecimal amount;

    @JsonProperty("date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Nullable
    private LocalDate date;

    @JsonProperty("comment")
    @Nullable
    private String comment;

    @JsonProperty("created")
    @Nullable
    private LocalDateTime created;

    @JsonProperty("lastEdit")
    @Nullable
    private LocalDateTime lastEdit;

    @JsonProperty("targetAccountName")
    @Nullable
    private String targetAccountName;

    @JsonProperty("sourceAccountName")
    @Nullable
    private String sourceAccountName;

    @JsonProperty("username")
    @Nullable
    private String username;

    public PaymentDto() {
        // Default constructor for Jackson
    }

    /** Creates a new instance. */
    public PaymentDto(@Nullable final Long userId, @Nullable final Long sourceAccountId,
            @Nullable final Long targetAccountId, @Nullable final BigDecimal amount, @Nullable final LocalDate date,
            @Nullable final String comment, @Nullable final LocalDateTime created,
            @Nullable final LocalDateTime lastEdit) {
        this.userId = userId;
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.amount = amount;
        this.date = date;
        this.comment = comment;
        this.created = created;
        this.lastEdit = lastEdit;
    }

    @Nullable
    public Long getId() {
        return this.id;
    }

    @Nullable
    public Long getUserId() {
        return this.userId;
    }

    @Nullable
    public Long getSourceAccountId() {
        return this.sourceAccountId;
    }

    @Nullable
    public Long getTargetAccountId() {
        return this.targetAccountId;
    }

    @Nullable
    public BigDecimal getAmount() {
        return this.amount;
    }

    @Nullable
    public LocalDate getDate() {
        return this.date;
    }

    @Nullable
    public String getComment() {
        return this.comment;
    }

    @Nullable
    public LocalDateTime getCreated() {
        return this.created;
    }

    @Nullable
    public LocalDateTime getLastEdit() {
        return this.lastEdit;
    }

    @Nullable
    public String getTargetAccountName() {
        return this.targetAccountName;
    }

    @Nullable
    public String getSourceAccountName() {
        return this.sourceAccountName;
    }

    @Nullable
    public String getUsername() {
        return this.username;
    }

    public void setId(@Nullable final Long id) {
        this.id = id;
    }

    public void setUserId(@Nullable final Long userId) {
        this.userId = userId;
    }

    public void setSourceAccountId(@Nullable final Long sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public void setTargetAccountId(@Nullable final Long targetAccountId) {
        this.targetAccountId = targetAccountId;
    }

    public void setAmount(@Nullable final BigDecimal amount) {
        this.amount = amount;
    }

    public void setDate(@Nullable final LocalDate paymentDate) {
        this.date = paymentDate;
    }

    public void setComment(@Nullable final String description) {
        this.comment = description;
    }

    public void setCreated(@Nullable final LocalDateTime created) {
        this.created = created;
    }

    public void setLastEdit(@Nullable final LocalDateTime lastEdit) {
        this.lastEdit = lastEdit;
    }

    public void setTargetAccountName(@Nullable final String targetAccountName) {
        this.targetAccountName = targetAccountName;
    }

    public void setSourceAccountName(@Nullable final String sourceAccountName) {
        this.sourceAccountName = sourceAccountName;
    }

    public void setUsername(@Nullable final String username) {
        this.username = username;
    }

}
