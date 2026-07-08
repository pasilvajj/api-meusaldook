package com.economy.finance.service;

import com.economy.finance.api.dto.TransactionResponse;
import com.economy.finance.domain.AccountType;
import com.economy.finance.domain.FinanceTransaction;
import com.economy.finance.domain.MoneyKind;
import com.economy.finance.domain.RecurringPeriodicity;
import com.economy.finance.domain.RecurringTransaction;
import com.economy.finance.service.FixedExpenseParser.FixedExpenseMeta;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecurringProjectionService {

    private final RecurringProjectionRequestCache projectionCache;

    public List<TransactionResponse> project(
            Long userId,
            Instant rangeFrom,
            Instant rangeToExclusive,
            Long categoryId,
            MoneyKind kind,
            String accountPublicKey) {
        return project(userId, rangeFrom, rangeToExclusive, categoryId, kind, accountPublicKey, false);
    }

    public List<TransactionResponse> project(
            Long userId,
            Instant rangeFrom,
            Instant rangeToExclusive,
            Long categoryId,
            MoneyKind kind,
            String accountPublicKey,
            boolean excludeCreditCards) {
        List<TransactionResponse> projected = new ArrayList<>();
        Set<String> occupiedDays = new HashSet<>();
        Map<String, Instant> paidAtByDayKey = projectionCache.paidAtMap(userId, rangeFrom, rangeToExclusive);
        List<RecurringTransaction> activeRules = projectionCache.activeRules(userId);
        Set<String> activeRecurringKeys = new HashSet<>();

        for (RecurringTransaction rule : activeRules) {
            activeRecurringKeys.add(groupKey(
                    rule.getCategory().getId(),
                    rule.getAccount().getPublicKey(),
                    rule.getAmount()));
            if (!matchesFilters(rule, categoryId, kind, accountPublicKey, excludeCreditCards)) {
                continue;
            }
            appendProjections(projected, occupiedDays, paidAtByDayKey, rule, null, rangeFrom, rangeToExclusive);
        }

        for (FinanceTransaction anchor : projectionCache.fixedAnchors(userId)) {
            if (!matchesFilters(anchor, categoryId, kind, accountPublicKey, excludeCreditCards)) {
                continue;
            }
            String anchorKey = groupKey(
                    anchor.getCategory().getId(),
                    anchor.getAccount().getPublicKey(),
                    anchor.getAmount());
            if (activeRecurringKeys.contains(anchorKey)) {
                continue;
            }
            FixedExpenseMeta meta = FixedExpenseParser.parse(anchor.getDescription()).orElse(null);
            if (meta == null) {
                continue;
            }
            RecurringPeriodicity periodicity = RecurringPeriodicity.valueOf(meta.periodicity());
            appendProjections(
                    projected,
                    occupiedDays,
                    paidAtByDayKey,
                    null,
                    anchor,
                    rangeFrom,
                    rangeToExclusive,
                    periodicity,
                    meta.everyN(),
                    meta.maxOccurrences());
        }

        return projected;
    }

    private void appendProjections(
            List<TransactionResponse> out,
            Set<String> occupiedDays,
            Map<String, Instant> paidAtByDayKey,
            RecurringTransaction rule,
            FinanceTransaction legacyAnchor,
            Instant rangeFrom,
            Instant rangeToExclusive) {
        if (rule == null) {
            return;
        }
        appendProjections(
                out,
                occupiedDays,
                paidAtByDayKey,
                rule,
                legacyAnchor,
                rangeFrom,
                rangeToExclusive,
                rule.getPeriodicity(),
                rule.getEveryN(),
                rule.getMaxOccurrences());
    }

    private void appendProjections(
            List<TransactionResponse> out,
            Set<String> occupiedDays,
            Map<String, Instant> paidAtByDayKey,
            RecurringTransaction rule,
            FinanceTransaction legacyAnchor,
            Instant rangeFrom,
            Instant rangeToExclusive,
            RecurringPeriodicity periodicity,
            int everyN,
            Integer maxOccurrences) {
        Instant startAt = rule != null ? rule.getStartAt() : legacyAnchor.getOccurredAt();
        List<Instant> dates =
                RecurringScheduleExpander.occurrencesInRange(
                        startAt, periodicity, everyN, maxOccurrences, rangeFrom, rangeToExclusive);

        int index = 0;
        for (Instant occurredAt : dates) {
            String dayKey = dayKey(occurredAt, rule, legacyAnchor);
            if (!occupiedDays.add(dayKey)) {
                index++;
                continue;
            }
            if (legacyAnchor != null && sameDay(occurredAt, legacyAnchor.getOccurredAt())) {
                index++;
                continue;
            }
            if (paidAtByDayKey.containsKey(dayKey)) {
                index++;
                continue;
            }
            out.add(buildProjected(rule, legacyAnchor, occurredAt, index, paidAtByDayKey));
            index++;
        }
    }

    private TransactionResponse buildProjected(
            RecurringTransaction rule,
            FinanceTransaction legacyAnchor,
            Instant occurredAt,
            int occurrenceIndex,
            Map<String, Instant> paidAtByDayKey) {
        Instant paidAt = paidAtByDayKey.get(dayKey(occurredAt, rule, legacyAnchor));
        Long recurringId = rule != null ? rule.getId() : null;
        Long sourceTransactionId = legacyAnchor != null ? legacyAnchor.getId() : null;
        long syntheticId = projectedId(recurringId, sourceTransactionId, occurrenceIndex);

        if (rule != null) {
            return TransactionResponse.builder()
                    .id(syntheticId)
                    .amount(rule.getAmount())
                    .kind(rule.getKind())
                    .categoryId(rule.getCategory().getId())
                    .categoryName(rule.getCategory().getName())
                    .accountPublicKey(rule.getAccount().getPublicKey())
                    .accountName(rule.getAccount().getName())
                    .description(rule.getDescription())
                    .occurredAt(occurredAt)
                    .createdAt(rule.getCreatedAt())
                    .projected(true)
                    .recurringId(rule.getId())
                    .sourceTransactionId(null)
                    .occurrenceIndex(occurrenceIndex)
                    .showInPayables(rule.isShowInPayables())
                    .paidAt(paidAt)
                    .build();
        }

        return TransactionResponse.builder()
                .id(syntheticId)
                .amount(legacyAnchor.getAmount())
                .kind(legacyAnchor.getKind())
                .categoryId(legacyAnchor.getCategory().getId())
                .categoryName(legacyAnchor.getCategory().getName())
                .accountPublicKey(legacyAnchor.getAccount().getPublicKey())
                .accountName(legacyAnchor.getAccount().getName())
                .description(legacyAnchor.getDescription())
                .occurredAt(occurredAt)
                .createdAt(legacyAnchor.getCreatedAt())
                .projected(true)
                .recurringId(null)
                .sourceTransactionId(legacyAnchor.getId())
                .occurrenceIndex(occurrenceIndex)
                .showInPayables(legacyAnchor.isShowInPayables())
                .paidAt(paidAt)
                .build();
    }

    static long projectedId(Long recurringId, Long sourceTransactionId, int occurrenceIndex) {
        long base =
                recurringId != null
                        ? recurringId
                        : sourceTransactionId != null ? sourceTransactionId : 0L;
        return -(base * 10_000L + occurrenceIndex);
    }

    private static String dayKey(Instant occurredAt, RecurringTransaction rule, FinanceTransaction legacyAnchor) {
        ZonedDateTime z = occurredAt.atZone(ZoneOffset.UTC);
        String day = z.toLocalDate().toString();
        if (rule != null) {
            return "r:" + rule.getId() + ":" + day;
        }
        return "l:" + legacyAnchor.getId() + ":" + day;
    }

    private static boolean sameDay(Instant a, Instant b) {
        return a.atZone(ZoneOffset.UTC).toLocalDate().equals(b.atZone(ZoneOffset.UTC).toLocalDate());
    }

    private static boolean matchesFilters(
            RecurringTransaction rule,
            Long categoryId,
            MoneyKind kind,
            String accountPublicKey,
            boolean excludeCreditCards) {
        if (categoryId != null && !categoryId.equals(rule.getCategory().getId())) {
            return false;
        }
        if (kind != null && kind != rule.getKind()) {
            return false;
        }
        if (accountPublicKey != null
                && !accountPublicKey.isBlank()
                && !accountPublicKey.trim().equals(rule.getAccount().getPublicKey())) {
            return false;
        }
        if (excludeCreditCards && rule.getAccount().getAccountType() == AccountType.CREDIT_CARD) {
            return false;
        }
        return true;
    }

    private static String groupKey(Long categoryId, String accountPublicKey, BigDecimal amount) {
        return categoryId + "|" + accountPublicKey + "|" + amount.stripTrailingZeros().toPlainString();
    }

    private static boolean matchesFilters(
            FinanceTransaction tx,
            Long categoryId,
            MoneyKind kind,
            String accountPublicKey,
            boolean excludeCreditCards) {
        if (categoryId != null && !categoryId.equals(tx.getCategory().getId())) {
            return false;
        }
        if (kind != null && kind != tx.getKind()) {
            return false;
        }
        if (accountPublicKey != null
                && !accountPublicKey.isBlank()
                && !accountPublicKey.trim().equals(tx.getAccount().getPublicKey())) {
            return false;
        }
        if (excludeCreditCards && tx.getAccount().getAccountType() == AccountType.CREDIT_CARD) {
            return false;
        }
        return true;
    }
}
