package com.economy.finance.api.dto;

import com.economy.finance.domain.MoneyKind;
import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BudgetGoalMonthResponse {
    int year;
    int month;
    MoneyKind kind;
    List<Row> rows;

    @Value
    @Builder
    public static class Row {
        Long categoryId;
        String categoryName;
        BigDecimal currentGoal;
        BigDecimal previousMonthGoal;
        BigDecimal previousYearSameMonthGoal;
    }
}
