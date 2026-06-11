package com.economy.finance.api.dto;

import com.economy.finance.domain.MoneyKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class BulkBudgetGoalRequest {

    @NotNull
    @Min(2000)
    @Max(2100)
    private Integer year;

    @NotNull
    @Min(1)
    @Max(12)
    private Integer month;

    @NotNull
    private MoneyKind kind;

    @NotNull
    @Valid
    private List<Line> lines;

    @Data
    public static class Line {

        @NotNull
        private Long categoryId;

        @NotNull
        @DecimalMin(value = "0.0", inclusive = true)
        private BigDecimal amount;
    }
}
