package de.vptr.midas.api.dto;

import java.math.BigDecimal;

/** Response DTO for an account balance. */
public record BalanceResponseDto(BigDecimal balance) {
}
