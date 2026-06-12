package com.economy.finance.api.dto;

import com.economy.finance.domain.MoneyKind;
import com.economy.finance.domain.RecurringPeriodicity;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;

@Data
public class RecurringTransactionRequest {

    @NotNull
    @DecimalMin(value = "0.0001", inclusive = true)
    private BigDecimal amount;

    @NotNull
    private MoneyKind kind;

    @NotNull
    private Long categoryId;

    @Size(max = 64)
    private String accountPublicKey;

    @Size(max = 1024)
    private String description;

    @NotNull
    private Instant startAt;

    @NotNull
    private RecurringPeriodicity periodicity;

    @Min(1)
    @Max(120)
    private int everyN = 1;

  @Min(1)
  @Max(999)
  private Integer maxOccurrences;

  private Boolean showInPayables;
}
