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

    @JsonProperty("publicId")
    private String publicId;

    @JsonProperty("targetAccountPublicId")
    private String targetAccountPublicId;

    @JsonProperty("targetAccountName")
    private String targetAccountName;

    @JsonProperty("sourceAccountPublicId")
    private String sourceAccountPublicId;

    @JsonProperty("sourceAccountName")
    private String sourceAccountName;

    @JsonProperty("userPublicId")
    private String userPublicId;

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

    @SuppressWarnings({ "NullAway", "PMD.UnnecessaryConstructor" })
    public PaymentResponseDto() {
        // Default constructor for Jackson
    }

    public String getPublicId() {
        return this.publicId;
    }

    public String getTargetAccountPublicId() {
        return this.targetAccountPublicId;
    }

    public String getTargetAccountName() {
        return this.targetAccountName;
    }

    public String getSourceAccountPublicId() {
        return this.sourceAccountPublicId;
    }

    public String getSourceAccountName() {
        return this.sourceAccountName;
    }

    public String getUserPublicId() {
        return this.userPublicId;
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

    public void setPublicId(final String publicId) {
        this.publicId = publicId;
    }

    public void setTargetAccountPublicId(final String targetAccountPublicId) {
        this.targetAccountPublicId = targetAccountPublicId;
    }

    public void setTargetAccountName(final String targetAccountName) {
        this.targetAccountName = targetAccountName;
    }

    public void setSourceAccountPublicId(final String sourceAccountPublicId) {
        this.sourceAccountPublicId = sourceAccountPublicId;
    }

    public void setSourceAccountName(final String sourceAccountName) {
        this.sourceAccountName = sourceAccountName;
    }

    public void setUserPublicId(final String userPublicId) {
        this.userPublicId = userPublicId;
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
        dto.setPublicId(this.publicId);
        dto.setSourceAccountPublicId(this.sourceAccountPublicId);
        dto.setSourceAccountName(this.sourceAccountName);
        dto.setTargetAccountPublicId(this.targetAccountPublicId);
        dto.setTargetAccountName(this.targetAccountName);
        dto.setUsername(this.username);
        dto.setComment(this.comment);
        dto.setDate(this.date);
        dto.setAmount(this.amount);
        dto.setCreated(this.created);
        dto.setLastEdit(this.lastEdit);
        return dto;
    }

}
