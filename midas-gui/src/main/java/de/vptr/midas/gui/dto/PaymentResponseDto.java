package de.vptr.midas.gui.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for payment operations. Contains computed fields and safe data for client responses.
 */
public class PaymentResponseDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("targetAccountId")
    private Long targetAccountId;

    @JsonProperty("targetAccountName")
    private String targetAccountName;

    @JsonProperty("sourceAccountId")
    private Long sourceAccountId;

    @JsonProperty("sourceAccountName")
    private String sourceAccountName;

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("created")
    private LocalDateTime created;

    @JsonProperty("lastEdit")
    private LocalDateTime lastEdit;

    public PaymentResponseDto() {
        // Default constructor for Jackson
    }

    public Long getId() {
        return this.id;
    }

    public Long getTargetAccountId() {
        return this.targetAccountId;
    }

    public String getTargetAccountName() {
        return this.targetAccountName;
    }

    public Long getSourceAccountId() {
        return this.sourceAccountId;
    }

    public String getSourceAccountName() {
        return this.sourceAccountName;
    }

    public Long getUserId() {
        return this.userId;
    }

    public String getUsername() {
        return this.username;
    }

    public String getComment() {
        return this.comment;
    }

    public LocalDate getDate() {
        return this.date;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public LocalDateTime getCreated() {
        return this.created;
    }

    public LocalDateTime getLastEdit() {
        return this.lastEdit;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setTargetAccountId(final Long targetAccountId) {
        this.targetAccountId = targetAccountId;
    }

    public void setTargetAccountName(final String targetAccountName) {
        this.targetAccountName = targetAccountName;
    }

    public void setSourceAccountId(final Long sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public void setSourceAccountName(final String sourceAccountName) {
        this.sourceAccountName = sourceAccountName;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setComment(final String comment) {
        this.comment = comment;
    }

    public void setDate(final LocalDate date) {
        this.date = date;
    }

    public void setAmount(final BigDecimal amount) {
        this.amount = amount;
    }

    public void setCreated(final LocalDateTime created) {
        this.created = created;
    }

    public void setLastEdit(final LocalDateTime lastEdit) {
        this.lastEdit = lastEdit;
    }

    /** Handles the to payment dto operation. */
    public PaymentDto toPaymentDto() {
        final PaymentDto dto = new PaymentDto();
        dto.setId(this.id);
        dto.setSourceAccountId(this.sourceAccountId);
        dto.setSourceAccountName(this.sourceAccountName);
        dto.setTargetAccountId(this.targetAccountId);
        dto.setTargetAccountName(this.targetAccountName);
        dto.setUserId(this.userId);
        dto.setUsername(this.username);
        dto.setComment(this.comment);
        dto.setDate(this.date);
        dto.setAmount(this.amount);
        dto.setCreated(this.created);
        dto.setLastEdit(this.lastEdit);
        return dto;
    }

}
