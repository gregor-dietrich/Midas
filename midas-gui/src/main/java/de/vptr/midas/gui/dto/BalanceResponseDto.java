package de.vptr.midas.gui.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.annotation.Nullable;

/** Response DTO for account balance operations. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceResponseDto {

    @JsonProperty("balance")
    @Nullable
    private BigDecimal balance;

    @Nullable
    public BigDecimal getBalance() {
        return this.balance;
    }

    public void setBalance(@Nullable final BigDecimal balance) {
        this.balance = balance;
    }
}
