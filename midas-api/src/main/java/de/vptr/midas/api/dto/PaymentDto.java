package de.vptr.midas.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Data transfer object for payment data. */
public class PaymentDto {
    // At least one of targetAccountPublicId or sourceAccountPublicId must be provided
    // Custom validation will be handled in the service layer
    @Nullable
    public String targetAccountPublicId;

    @Nullable
    public String sourceAccountPublicId;

    @NotBlank(message = "Comment is required")
    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    @Nullable
    public String comment;

    @SuppressWarnings("MultipleNullnessAnnotations")
    @NotNull(message = "Date is required")
    @Nullable
    public LocalDate date;

    @SuppressWarnings("MultipleNullnessAnnotations")
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Amount must be positive")
    @Nullable
    public BigDecimal amount;
}
