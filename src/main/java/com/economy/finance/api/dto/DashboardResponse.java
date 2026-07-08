package com.economy.finance.api.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DashboardResponse {
    MonthlySummaryResponse summary;
    AccountResponse account;
    List<AccountResponse> accounts;
    BudgetGoalMonthResponse goals;
    List<TransactionResponse> monthTransactions;
    List<TransactionResponse> scheduledPayables;
    List<TransactionResponse> scheduledReceivables;
}
