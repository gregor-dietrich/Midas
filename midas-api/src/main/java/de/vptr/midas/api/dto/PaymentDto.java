package de.vptr.midas.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Data transfer object for payment data. */
public class PaymentDto {
    // At least one of targetAccountId or sourceAccountId must be provided
    // Custom validation will be handled in the service layer
    public Long targetAccountId;

    public Long sourceAccountId;

    @NotNull(message = "User ID is required")
    public Long userId;

    @NotBlank(message = "Comment is required")
    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    public String comment;

    @NotNull(message = "Date is required")
    public LocalDate date;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Amount must be positive")
    public BigDecimal amount;
}
