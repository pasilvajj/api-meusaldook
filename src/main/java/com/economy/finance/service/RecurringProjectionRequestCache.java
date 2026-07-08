package com.economy.finance.service;

import com.economy.finance.domain.FinanceTransaction;
import com.economy.finance.domain.RecurringOccurrencePayment;
import com.economy.finance.domain.RecurringTransaction;
import com.economy.finance.persistence.FinanceTransactionRepository;
import com.economy.finance.persistence.RecurringOccurrencePaymentRepository;
import com.economy.finance.persistence.RecurringTransactionRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/** Evita recarregar regras/âncoras fixas a cada chamada de projeção no mesmo pedido HTTP. */
@Component
@RequestScope
@RequiredArgsConstructor
class RecurringProjectionRequestCache {

    private final RecurringOccurrencePaymentRepository occurrencePaymentRepository;
    private final RecurringTransactionRepository recurringRepository;
    private final FinanceTransactionRepository transactionRepository;

    private Long cachedUserId;
    private List<RecurringTransaction> activeRules;
    private List<FinanceTransaction> fixedAnchors;
    private final Map<String, Map<String, Instant>> paidAtByRangeKey = new HashMap<>();

    List<RecurringTransaction> activeRules(Long userId) {
        ensureUserDataLoaded(userId);
        return activeRules;
    }

    List<FinanceTransaction> fixedAnchors(Long userId) {
        ensureUserDataLoaded(userId);
        return fixedAnchors;
    }

    Map<String, Instant> paidAtMap(Long userId, Instant rangeFrom, Instant rangeToExclusive) {
        ensureUserDataLoaded(userId);
        String rangeKey = rangeFrom.toEpochMilli() + "|" + rangeToExclusive.toEpochMilli();
        return paidAtByRangeKey.computeIfAbsent(
                rangeKey, ignored -> loadPaidAtMap(userId, rangeFrom, rangeToExclusive));
    }

    private void ensureUserDataLoaded(Long userId) {
        if (userId.equals(cachedUserId) && activeRules != null) {
            return;
        }
        cachedUserId = userId;
        activeRules = recurringRepository.findByOwner_IdAndActiveTrue(userId);
        fixedAnchors = transactionRepository.findFixedExpenseAnchors(userId);
        paidAtByRangeKey.clear();
    }

    private Map<String, Instant> loadPaidAtMap(Long userId, Instant rangeFrom, Instant rangeToExclusive) {
        LocalDate fromDate = rangeFrom.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate toDate = rangeToExclusive.atZone(ZoneOffset.UTC).toLocalDate();
        if (!toDate.isAfter(fromDate)) {
            toDate = fromDate.plusDays(1);
        }
        Map<String, Instant> paidAtByDayKey = new HashMap<>();
        for (RecurringOccurrencePayment payment :
                occurrencePaymentRepository.findByOwner_IdAndOccurrenceDateInRange(userId, fromDate, toDate)) {
            if (payment.getRecurring() != null) {
                paidAtByDayKey.put(
                        "r:" + payment.getRecurring().getId() + ":" + payment.getOccurrenceDate(),
                        payment.getPaidAt());
            } else if (payment.getLegacyTransaction() != null) {
                paidAtByDayKey.put(
                        "l:" + payment.getLegacyTransaction().getId() + ":" + payment.getOccurrenceDate(),
                        payment.getPaidAt());
            }
        }
        return paidAtByDayKey;
    }
}
