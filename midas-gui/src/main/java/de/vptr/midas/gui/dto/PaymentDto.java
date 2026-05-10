package de.vptr.midas.gui.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Data transfer object for payment. */
public class PaymentDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("sourceAccountId")
    private Long sourceAccountId;

    @JsonProperty("targetAccountId")
    private Long targetAccountId;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @JsonProperty("comment")
    private String comment;

    @JsonProperty("created")
    private LocalDateTime created;

    @JsonProperty("lastEdit")
    private LocalDateTime lastEdit;

    @JsonProperty("targetAccountName")
    private String targetAccountName;

    @JsonProperty("sourceAccountName")
    private String sourceAccountName;

    @JsonProperty("username")
    private String username;

    public PaymentDto() {
        // Default constructor for Jackson
    }

    /** Creates a new instance. */
    public PaymentDto(final Long userId, final Long sourceAccountId, final Long targetAccountId,
            final BigDecimal amount, final LocalDate date, final String comment, final LocalDateTime created,
            final LocalDateTime lastEdit) {
        this.userId = userId;
        this.sourceAccountId = sourceAccountId;
        this.targetAccountId = targetAccountId;
        this.amount = amount;
        this.date = date;
        this.comment = comment;
        this.created = created;
        this.lastEdit = lastEdit;
    }

    public Long getId() {
        return this.id;
    }

    public Long getUserId() {
        return this.userId;
    }

    public Long getSourceAccountId() {
        return this.sourceAccountId;
    }

    public Long getTargetAccountId() {
        return this.targetAccountId;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public LocalDate getDate() {
        return this.date;
    }

    public String getComment() {
        return this.comment;
    }

    public LocalDateTime getCreated() {
        return this.created;
    }

    public LocalDateTime getLastEdit() {
        return this.lastEdit;
    }

    public String getTargetAccountName() {
        return this.targetAccountName;
    }

    public String getSourceAccountName() {
        return this.sourceAccountName;
    }

    public String getUsername() {
        return this.username;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public void setSourceAccountId(final Long sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public void setTargetAccountId(final Long targetAccountId) {
        this.targetAccountId = targetAccountId;
    }

    public void setAmount(final BigDecimal amount) {
        this.amount = amount;
    }

    public void setDate(final LocalDate paymentDate) {
        this.date = paymentDate;
    }

    public void setComment(final String description) {
        this.comment = description;
    }

    public void setCreated(final LocalDateTime created) {
        this.created = created;
    }

    public void setLastEdit(final LocalDateTime lastEdit) {
        this.lastEdit = lastEdit;
    }

    public void setTargetAccountName(final String targetAccountName) {
        this.targetAccountName = targetAccountName;
    }

    public void setSourceAccountName(final String sourceAccountName) {
        this.sourceAccountName = sourceAccountName;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

}
