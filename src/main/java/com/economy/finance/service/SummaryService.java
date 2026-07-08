package com.economy.finance.service;

import com.economy.finance.api.dto.MonthlySummaryResponse;
import com.economy.finance.domain.AccountType;
import com.economy.finance.domain.FinanceTransaction;
import com.economy.finance.domain.MoneyKind;
import com.economy.finance.persistence.FinanceTransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final FinanceTransactionRepository transactionRepository;
    private final CurrentUserService currentUserService;

    /**
     * Resumo mensal da visão principal (conta corrente): despesas em dinheiro / pagamentos efetivos.
     * Compras no cartão de crédito não entram aqui — ficam na fatura do cartão até o pagamento ser lançado.
     */
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

        BigDecimal invoicePayments = transactionRepository.sumInvoicePaymentsForPeriod(userId, from, to, accPk);
        if (invoicePayments == null) {
            invoicePayments = BigDecimal.ZERO;
        }

        return buildMonthlyResponse(
                year, month, kindTotals, expenseByCategory, incomeByCategory, invoicePayments);
    }

    /** Resumo a partir de transações já carregadas (evita reconsultar agregações no dashboard). */
    public MonthlySummaryResponse summarizeFromEntities(
            List<FinanceTransaction> transactions, int year, int month, String accountPublicKey) {
        String accPk = blankToNull(accountPublicKey);

        Map<MoneyKind, BigDecimal> kindTotals = new LinkedHashMap<>();
        Map<String, BigDecimal> expenseByCategory = new LinkedHashMap<>();
        Map<String, BigDecimal> incomeByCategory = new LinkedHashMap<>();
        BigDecimal invoicePayments = BigDecimal.ZERO;

        for (FinanceTransaction t : transactions) {
            if (!matchesAccountFilter(t, accPk)) {
                continue;
            }
            kindTotals.merge(t.getKind(), t.getAmount(), BigDecimal::add);

            String desc = t.getDescription() != null ? t.getDescription().toLowerCase() : "";
            boolean isInvoicePayment = desc.startsWith("pagamento fatura");

            if (t.getKind() == MoneyKind.EXPENSE && isInvoicePayment) {
                invoicePayments = invoicePayments.add(t.getAmount());
                continue;
            }
            if (t.getKind() == MoneyKind.EXPENSE) {
                expenseByCategory.merge(t.getCategory().getName(), t.getAmount(), BigDecimal::add);
            } else if (t.getKind() == MoneyKind.INCOME) {
                incomeByCategory.merge(t.getCategory().getName(), t.getAmount(), BigDecimal::add);
            }
        }

        return buildMonthlyResponse(year, month, kindTotals, expenseByCategory, incomeByCategory, invoicePayments);
    }

    private static boolean matchesAccountFilter(FinanceTransaction t, String accPk) {
        String pk = t.getAccount().getPublicKey();
        AccountType type = t.getAccount().getAccountType();
        if (accPk == null) {
            return type != AccountType.CREDIT_CARD;
        }
        if ("principal".equals(accPk)) {
            return "principal".equals(pk);
        }
        return accPk.equals(pk);
    }

    private MonthlySummaryResponse buildMonthlyResponse(
            int year,
            int month,
            Map<MoneyKind, BigDecimal> kindTotals,
            Map<String, BigDecimal> expenseByCategory,
            Map<String, BigDecimal> incomeByCategory,
            BigDecimal invoicePayments) {
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
                .invoicePaymentTotal(invoicePayments != null ? invoicePayments : BigDecimal.ZERO)
                .build();
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
