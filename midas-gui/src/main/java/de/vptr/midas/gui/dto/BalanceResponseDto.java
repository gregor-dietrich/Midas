package de.vptr.midas.gui.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Response DTO for account balance operations. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalanceResponseDto {

    @JsonProperty("balance")
    private BigDecimal balance;

    public BigDecimal getBalance() {
        return this.balance;
    }

    public void setBalance(final BigDecimal balance) {
        this.balance = balance;
    }
}
