package com.economy.finance.service;

import com.economy.finance.domain.UserAccount;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.YearMonth;
import java.util.Optional;

/** Ciclo de fatura do cartão (espelha o util TypeScript do frontend). */
public final class CreditCardInvoiceCycle {

    public record Cycle(
            LocalDate closingDate,
            LocalDate dueDate,
            LocalDate periodStart,
            LocalDate periodEnd) {}

    private CreditCardInvoiceCycle() {}

    public static Optional<YearMonth> viewMonthFromAccount(UserAccount account) {
        LocalDate next = account.getCreditCardNextInvoiceDate();
        if (next == null) {
            return Optional.empty();
        }
        return Optional.of(YearMonth.from(next));
    }

    public static Optional<Cycle> cycleForViewMonth(UserAccount account, int year, int month) {
        Short dueDayRaw = account.getCreditCardDueDay();
        if (dueDayRaw == null || dueDayRaw < 1 || dueDayRaw > 31) {
            return Optional.empty();
        }
        int dueDay = dueDayRaw.intValue();
        int closingDays = account.getCreditCardClosingDaysBeforeDue() != null
                ? account.getCreditCardClosingDaysBeforeDue().intValue()
                : 10;

        YearMonth ym = YearMonth.of(year, month);
        LocalDate due = ym.atDay(Math.min(dueDay, ym.lengthOfMonth()));
        LocalDate closing = due.minusDays(closingDays);

        YearMonth prev = ym.minusMonths(1);
        LocalDate prevDue = prev.atDay(Math.min(dueDay, prev.lengthOfMonth()));
        LocalDate prevClosing = prevDue.minusDays(closingDays);

        LocalDate periodStart = prevClosing.plusDays(1);
        LocalDate periodEnd = closing;

        return Optional.of(new Cycle(closing, due, periodStart, periodEnd));
    }

    /** Fatura aberta: compras de hoje entram mesmo antes do início formal do ciclo. */
    public static Optional<Cycle> effectiveOpenCycle(UserAccount account, LocalDate referenceToday) {
        Optional<YearMonth> viewMonth = viewMonthFromAccount(account);
        if (viewMonth.isEmpty()) {
            return Optional.empty();
        }
        YearMonth ym = viewMonth.get();
        Optional<Cycle> cycle = cycleForViewMonth(account, ym.getYear(), ym.getMonthValue());
        if (cycle.isEmpty()) {
            return Optional.empty();
        }
        Cycle c = cycle.get();
        if (referenceToday.isBefore(c.periodStart())) {
            return Optional.of(new Cycle(c.closingDate(), c.dueDate(), referenceToday, c.periodEnd()));
        }
        return cycle;
    }

    public static InstantRange paymentQueryRange(Cycle cycle) {
        ZoneOffset utc = ZoneOffset.UTC;
        Instant from = cycle.periodStart().minusDays(31).atStartOfDay().toInstant(utc);
        Instant to = cycle.dueDate().plusDays(31).atStartOfDay().toInstant(utc);
        return new InstantRange(from, to);
    }

    public static InstantRange cycleInstantRange(Cycle cycle) {
        ZoneOffset utc = ZoneOffset.UTC;
        Instant from = cycle.periodStart().atStartOfDay().toInstant(utc);
        Instant to = cycle.periodEnd().plusDays(1).atStartOfDay().toInstant(utc);
        return new InstantRange(from, to);
    }

    public record InstantRange(Instant from, Instant to) {}
}
