package de.vptr.midas.gui.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.annotation.Nullable;

/** A single point of a balance-over-time series: the cumulative balance as of {@code date}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BalancePointDto {

    @JsonProperty("date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Nullable
    private LocalDate date;

    @JsonProperty("balance")
    @Nullable
    private BigDecimal balance;

    @Nullable
    public LocalDate getDate() {
        return this.date;
    }

    public void setDate(@Nullable final LocalDate date) {
        this.date = date;
    }

    @Nullable
    public BigDecimal getBalance() {
        return this.balance;
    }

    public void setBalance(@Nullable final BigDecimal balance) {
        this.balance = balance;
    }
}
