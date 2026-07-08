package com.economy.finance.service;

import com.economy.finance.api.dto.DashboardResponse;
import com.economy.finance.api.dto.TransactionResponse;
import com.economy.finance.domain.FinanceTransaction;
import com.economy.finance.domain.MoneyKind;
import com.economy.finance.persistence.FinanceTransactionRepository;
import com.economy.finance.persistence.FinanceTransactionSpecs;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SummaryService summaryService;
    private final AccountService accountService;
    private final BudgetGoalService budgetGoalService;
    private final FinanceTransactionRepository transactionRepository;
    private final RecurringProjectionService recurringProjectionService;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    @Cacheable(value = "dashboard", key = "@cacheKeyHelper.dashboard(#year, #month, #accountPublicKey)")
    public DashboardResponse load(int year, int month, String accountPublicKey) {
        Long userId = currentUserService.requireUserId();
        String accountKey = blankToPrincipal(accountPublicKey);

        ZoneOffset utc = ZoneOffset.UTC;
        ZonedDateTime monthStart = ZonedDateTime.of(year, month, 1, 0, 0, 0, 0, utc);
        Instant from = monthStart.toInstant();
        Instant monthEnd = monthStart.plusMonths(1).toInstant();

        Specification<FinanceTransaction> spec =
                FinanceTransactionSpecs.forUser(userId, from, monthEnd, null, null, accountKey);
        List<FinanceTransaction> persistedEntities =
                transactionRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "occurredAt"));
        List<TransactionResponse> persisted =
                persistedEntities.stream().map(TransactionResponse::from).toList();
        List<TransactionResponse> projected =
                recurringProjectionService.project(userId, from, monthEnd, null, null, accountKey);

        List<TransactionResponse> monthTransactions = new ArrayList<>();
        List<TransactionResponse> scheduledPayables = new ArrayList<>();
        List<TransactionResponse> scheduledReceivables = new ArrayList<>();
        for (TransactionResponse tx : mergeUnique(persisted, projected)) {
            Instant occurredAt = tx.getOccurredAt();
            if (!occurredAt.isBefore(from) && occurredAt.isBefore(monthEnd)) {
                monthTransactions.add(tx);
                if (shouldShowInPayables(tx) && tx.getPaidAt() == null) {
                    if (tx.getKind() == MoneyKind.EXPENSE) {
                        scheduledPayables.add(tx);
                    } else if (tx.getKind() == MoneyKind.INCOME) {
                        scheduledReceivables.add(tx);
                    }
                }
            }
        }

        return DashboardResponse.builder()
                .summary(summaryService.summarizeFromEntities(persistedEntities, year, month, accountKey))
                .account(accountService.getByPublicKey(accountKey))
                .accounts(accountService.listAll())
                .goals(budgetGoalService.getMonth(year, month, MoneyKind.EXPENSE))
                .monthTransactions(monthTransactions)
                .scheduledPayables(scheduledPayables)
                .scheduledReceivables(scheduledReceivables)
                .build();
    }

    private static boolean shouldShowInPayables(TransactionResponse tx) {
        return Boolean.TRUE.equals(tx.getShowInPayables());
    }

    private static String blankToPrincipal(String accountPublicKey) {
        if (accountPublicKey == null || accountPublicKey.isBlank()) {
            return "principal";
        }
        return accountPublicKey.trim();
    }

    private static List<TransactionResponse> mergeUnique(
            List<TransactionResponse> persisted, List<TransactionResponse> projected) {
        List<TransactionResponse> merged = new ArrayList<>(persisted);
        java.util.Set<Long> ids = new java.util.HashSet<>();
        for (TransactionResponse tx : persisted) {
            ids.add(tx.getId());
        }
        for (TransactionResponse tx : projected) {
            if (ids.add(tx.getId())) {
                merged.add(tx);
            }
        }
        return merged;
    }
}
