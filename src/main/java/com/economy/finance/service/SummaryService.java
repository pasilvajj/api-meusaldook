package com.economy.finance.service;

import com.economy.finance.api.dto.MonthlySummaryResponse;
import com.economy.finance.domain.AccountType;
import com.economy.finance.domain.MoneyKind;
import com.economy.finance.domain.UserAccount;
import com.economy.finance.persistence.FinanceTransactionRepository;
import com.economy.finance.persistence.UserAccountRepository;
import com.economy.finance.service.CreditCardInvoiceCycle.Cycle;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final FinanceTransactionRepository transactionRepository;
    private final UserAccountRepository userAccountRepository;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    @Cacheable(value = "monthlySummary", key = "@cacheKeyHelper.monthlySummary(#year, #month, #accountPublicKey)")
    public MonthlySummaryResponse monthly(int year, int month, String accountPublicKey) {
        Long userId = currentUserService.requireUserId();
        ZoneOffset utc = ZoneOffset.UTC;
        ZonedDateTime start = ZonedDateTime.of(year, month, 1, 0, 0, 0, 0, utc);
        ZonedDateTime end = start.plusMonths(1);
        Instant from = start.toInstant();
        Instant to = end.toInstant();

        String accPk = blankToNull(accountPublicKey);

        Map<MoneyKind, BigDecimal> kindTotals = new LinkedHashMap<>();
        for (Object[] row : transactionRepository.sumByKindForPeriod(userId, from, to, accPk)) {
            MoneyKind k = (MoneyKind) row[0];
            BigDecimal total = (BigDecimal) row[1];
            kindTotals.merge(k, total, BigDecimal::add);
        }

        Map<String, BigDecimal> expenseByCategory = new LinkedHashMap<>();
        for (Object[] row :
                transactionRepository.sumByCategoryForPeriod(userId, from, to, accPk, MoneyKind.EXPENSE)) {
            String name = (String) row[0];
            BigDecimal total = (BigDecimal) row[1];
            expenseByCategory.merge(name, total, BigDecimal::add);
        }

        Map<String, BigDecimal> incomeByCategory = new LinkedHashMap<>();
        for (Object[] row :
                transactionRepository.sumByCategoryForPeriod(userId, from, to, accPk, MoneyKind.INCOME)) {
            String name = (String) row[0];
            BigDecimal total = (BigDecimal) row[1];
            incomeByCategory.merge(name, total, BigDecimal::add);
        }

        if (accPk == null || "principal".equals(accPk)) {
            mergeOpenCreditCardTotals(userId, from, to, kindTotals, expenseByCategory);
        }

        List<MonthlySummaryResponse.KindTotal> byKind = kindTotals.entrySet().stream()
                .map(e -> MonthlySummaryResponse.KindTotal.builder()
                        .kind(e.getKey())
                        .total(e.getValue())
                        .build())
                .toList();

        List<MonthlySummaryResponse.CategoryTotal> byCategory = expenseByCategory.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry<String, BigDecimal>::getValue).reversed())
                .map(e -> MonthlySummaryResponse.CategoryTotal.builder()
                        .categoryName(e.getKey())
                        .total(e.getValue())
                        .build())
                .toList();

        List<MonthlySummaryResponse.CategoryTotal> byIncomeCategory = incomeByCategory.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry<String, BigDecimal>::getValue).reversed())
                .map(e -> MonthlySummaryResponse.CategoryTotal.builder()
                        .categoryName(e.getKey())
                        .total(e.getValue())
                        .build())
                .toList();

        return MonthlySummaryResponse.builder()
                .year(year)
                .month(month)
                .byKind(byKind)
                .byCategory(byCategory)
                .byIncomeCategory(byIncomeCategory)
                .build();
    }

    private void mergeOpenCreditCardTotals(
            Long userId,
            Instant monthFrom,
            Instant monthTo,
            Map<MoneyKind, BigDecimal> kindTotals,
            Map<String, BigDecimal> expenseByCategory) {
        List<UserAccount> accounts = userAccountRepository.findByOwner_IdOrderByAccountTypeAscNameAsc(userId);
        for (UserAccount account : accounts) {
            if (account.getAccountType() != AccountType.CREDIT_CARD || !account.isActive()) {
                continue;
            }
            Optional<Cycle> cycle = CreditCardInvoiceCycle.effectiveOpenCycle(account, LocalDate.now());
            if (cycle.isEmpty()) {
                continue;
            }
            Optional<InstantRange> range = intersectMonthWithCycle(cycle.get(), monthFrom, monthTo);
            if (range.isEmpty()) {
                continue;
            }

            for (Object[] row : transactionRepository.sumByKindForPeriod(
                    userId, range.get().from(), range.get().to(), account.getPublicKey())) {
                MoneyKind k = (MoneyKind) row[0];
                BigDecimal total = (BigDecimal) row[1];
                kindTotals.merge(k, total, BigDecimal::add);
            }
            for (Object[] row : transactionRepository.sumByCategoryForPeriod(
                    userId, range.get().from(), range.get().to(), account.getPublicKey(), MoneyKind.EXPENSE)) {
                String name = (String) row[0];
                BigDecimal total = (BigDecimal) row[1];
                expenseByCategory.merge(name, total, BigDecimal::add);
            }
        }
    }

    private static Optional<InstantRange> intersectMonthWithCycle(Cycle cycle, Instant monthFrom, Instant monthTo) {
        ZoneOffset utc = ZoneOffset.UTC;
        Instant cycleFrom = cycle.periodStart().atStartOfDay().toInstant(utc);
        Instant cycleToExclusive = cycle.periodEnd().plusDays(1).atStartOfDay().toInstant(utc);

        Instant effectiveFrom = monthFrom.isAfter(cycleFrom) ? monthFrom : cycleFrom;
        Instant effectiveTo = monthTo.isBefore(cycleToExclusive) ? monthTo : cycleToExclusive;
        if (!effectiveFrom.isBefore(effectiveTo)) {
            return Optional.empty();
        }
        return Optional.of(new InstantRange(effectiveFrom, effectiveTo));
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private record InstantRange(Instant from, Instant to) {}
}
