package de.vptr.midas.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.annotation.Nullable;

/** A single point of a balance-over-time series: the cumulative balance as of {@code date}. */
public class BalancePointDto {

    @Nullable
    @JsonFormat(pattern = "yyyy-MM-dd")
    public LocalDate date;

    @Nullable
    public BigDecimal balance;

    public BalancePointDto() {
    }

    /** Creates a new instance. */
    public BalancePointDto(final LocalDate date, final BigDecimal balance) {
        this.date = date;
        this.balance = balance;
    }
}
