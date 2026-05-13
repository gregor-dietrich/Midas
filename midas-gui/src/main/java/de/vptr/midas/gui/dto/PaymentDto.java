package de.vptr.midas.gui.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.annotation.Nullable;

/** Data transfer object for payment. */
public class PaymentDto {

    @JsonProperty("publicId")
    @Nullable
    private String publicId;

    @JsonProperty("userPublicId")
    @Nullable
    private String userPublicId;

    @JsonProperty("sourceAccountPublicId")
    @Nullable
    private String sourceAccountPublicId;

    @JsonProperty("targetAccountPublicId")
    @Nullable
    private String targetAccountPublicId;

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
    public PaymentDto(@Nullable final String userPublicId, @Nullable final String sourceAccountPublicId,
            @Nullable final String targetAccountPublicId, @Nullable final BigDecimal amount,
            @Nullable final LocalDate date, @Nullable final String comment, @Nullable final LocalDateTime created,
            @Nullable final LocalDateTime lastEdit) {
        this.userPublicId = userPublicId;
        this.sourceAccountPublicId = sourceAccountPublicId;
        this.targetAccountPublicId = targetAccountPublicId;
        this.amount = amount;
        this.date = date;
        this.comment = comment;
        this.created = created;
        this.lastEdit = lastEdit;
    }

    @Nullable
    public String getPublicId() {
        return this.publicId;
    }

    @Nullable
    public String getUserPublicId() {
        return this.userPublicId;
    }

    @Nullable
    public String getSourceAccountPublicId() {
        return this.sourceAccountPublicId;
    }

    @Nullable
    public String getTargetAccountPublicId() {
        return this.targetAccountPublicId;
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

    public void setPublicId(@Nullable final String publicId) {
        this.publicId = publicId;
    }

    public void setUserPublicId(@Nullable final String userPublicId) {
        this.userPublicId = userPublicId;
    }

    public void setSourceAccountPublicId(@Nullable final String sourceAccountPublicId) {
        this.sourceAccountPublicId = sourceAccountPublicId;
    }

    public void setTargetAccountPublicId(@Nullable final String targetAccountPublicId) {
        this.targetAccountPublicId = targetAccountPublicId;
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
