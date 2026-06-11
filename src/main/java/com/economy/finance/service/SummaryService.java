package com.economy.finance.service;

import com.economy.finance.api.dto.MonthlySummaryResponse;
import com.economy.finance.domain.MoneyKind;
import com.economy.finance.persistence.FinanceTransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final FinanceTransactionRepository transactionRepository;
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

        List<MonthlySummaryResponse.KindTotal> byKind = new ArrayList<>();
        for (Object[] row : transactionRepository.sumByKindForPeriod(userId, from, to, accPk)) {
            MoneyKind k = (MoneyKind) row[0];
            BigDecimal total = (BigDecimal) row[1];
            byKind.add(MonthlySummaryResponse.KindTotal.builder().kind(k).total(total).build());
        }

        List<MonthlySummaryResponse.CategoryTotal> byCategory = new ArrayList<>();
        for (Object[] row :
                transactionRepository.sumByCategoryForPeriod(userId, from, to, accPk, MoneyKind.EXPENSE)) {
            String name = (String) row[0];
            BigDecimal total = (BigDecimal) row[1];
            byCategory.add(
                    MonthlySummaryResponse.CategoryTotal.builder()
                            .categoryName(name)
                            .total(total)
                            .build());
        }

        List<MonthlySummaryResponse.CategoryTotal> byIncomeCategory = new ArrayList<>();
        for (Object[] row :
                transactionRepository.sumByCategoryForPeriod(userId, from, to, accPk, MoneyKind.INCOME)) {
            String name = (String) row[0];
            BigDecimal total = (BigDecimal) row[1];
            byIncomeCategory.add(
                    MonthlySummaryResponse.CategoryTotal.builder()
                            .categoryName(name)
                            .total(total)
                            .build());
        }

        return MonthlySummaryResponse.builder()
                .year(year)
                .month(month)
                .byKind(byKind)
                .byCategory(byCategory)
                .byIncomeCategory(byIncomeCategory)
                .build();
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
