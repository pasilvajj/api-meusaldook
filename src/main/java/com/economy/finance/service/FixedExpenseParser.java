package com.economy.finance.service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class FixedExpenseParser {

    private static final Pattern FIXA_META =
            Pattern.compile(
                    "\\[Fixa:\\s*(MENSAL|SEMANAL|TRIMESTRAL),\\s*a cada\\s*(\\d+)(?:,\\s*(\\d+)\\s*ocorrências)?\\]",
                    Pattern.CASE_INSENSITIVE);

    private FixedExpenseParser() {}

    static Optional<FixedExpenseMeta> parse(String description) {
        if (description == null || description.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = FIXA_META.matcher(description);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String periodicity = matcher.group(1).toUpperCase();
        int everyN = Integer.parseInt(matcher.group(2));
        Integer maxOccurrences = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : null;
        return Optional.of(new FixedExpenseMeta(periodicity, everyN, maxOccurrences));
    }

    record FixedExpenseMeta(String periodicity, int everyN, Integer maxOccurrences) {}
}
