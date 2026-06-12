package com.economy.finance.service;

import com.economy.finance.domain.RecurringPeriodicity;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

final class RecurringScheduleExpander {

    private static final int MAX_OCCURRENCES_PER_RULE = 500;

    private RecurringScheduleExpander() {}

    static List<Instant> occurrencesInRange(
            Instant startAt,
            RecurringPeriodicity periodicity,
            int everyN,
            Integer maxOccurrences,
            Instant rangeFrom,
            Instant rangeToExclusive) {
        int safeEvery = Math.max(1, everyN);
        int cap = maxOccurrences != null && maxOccurrences > 0
                ? Math.min(maxOccurrences, MAX_OCCURRENCES_PER_RULE)
                : MAX_OCCURRENCES_PER_RULE;

        ZoneOffset utc = ZoneOffset.UTC;
        ZonedDateTime anchor = startAt.atZone(utc);
        LocalDate anchorDate = anchor.toLocalDate();
        int hour = anchor.getHour();
        int minute = anchor.getMinute();
        int second = anchor.getSecond();
        int nano = anchor.getNano();

        List<Instant> out = new ArrayList<>();
        for (int index = 0; index < cap; index++) {
            LocalDate date = shiftDate(anchorDate, periodicity, safeEvery, index);
            ZonedDateTime occurrence = ZonedDateTime.of(date, anchor.toLocalTime(), utc);
            occurrence = occurrence.withHour(hour).withMinute(minute).withSecond(second).withNano(nano);
            Instant at = occurrence.toInstant();
            if (at.isBefore(startAt) && index > 0) {
                continue;
            }
            if (!at.isBefore(rangeToExclusive)) {
                break;
            }
            if (!at.isBefore(rangeFrom)) {
                out.add(at);
            }
            if (maxOccurrences != null && index + 1 >= maxOccurrences) {
                break;
            }
        }
        return out;
    }

    private static LocalDate shiftDate(
            LocalDate anchor, RecurringPeriodicity periodicity, int everyN, int step) {
        if (step == 0) {
            return anchor;
        }
        return switch (periodicity) {
            case SEMANAL -> anchor.plusWeeks((long) everyN * step);
            case TRIMESTRAL -> addMonths(anchor, 3 * everyN * step);
            case MENSAL -> addMonths(anchor, everyN * step);
        };
    }

    private static LocalDate addMonths(LocalDate date, int months) {
        LocalDate shifted = date.plusMonths(months);
        int day = Math.min(date.getDayOfMonth(), shifted.lengthOfMonth());
        return shifted.withDayOfMonth(day);
    }
}
