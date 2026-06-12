package com.economy.finance.service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class InstallmentParser {

    private static final Pattern PARCEL_LABEL =
            Pattern.compile("\\[Parcela (\\d+)/(\\d+)\\]", Pattern.CASE_INSENSITIVE);

    private InstallmentParser() {}

    static Optional<InstallmentMeta> parse(String description) {
        if (description == null || description.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = PARCEL_LABEL.matcher(description);
        if (!matcher.find()) {
            return Optional.empty();
        }
        int parcelNumber = Integer.parseInt(matcher.group(1));
        int totalParcels = Integer.parseInt(matcher.group(2));
        String base = description.substring(0, matcher.start()).trim();
        return Optional.of(new InstallmentMeta(parcelNumber, totalParcels, base));
    }

    static boolean sameInstallmentGroup(InstallmentMeta reference, String otherDescription) {
        return parse(otherDescription)
                .filter(other -> other.totalParcels() == reference.totalParcels())
                .filter(other -> other.base().equals(reference.base()))
                .isPresent();
    }

    record InstallmentMeta(int parcelNumber, int totalParcels, String base) {}
}
