package com.economy.finance.service;

import com.economy.finance.api.dto.AccountWriteRequest;
import com.economy.finance.domain.AccountType;
import java.time.LocalDate;

final class CreditCardInvoiceDates {

    private CreditCardInvoiceDates() {}

    static void applyCreditCardFields(
            com.economy.finance.domain.UserAccount entity, AccountWriteRequest request) {
        if (request.getAccountType() != AccountType.CREDIT_CARD) {
            entity.setCreditCardDueDay(null);
            entity.setCreditCardNextInvoiceDate(null);
            entity.setCreditCardClosingDaysBeforeDue(null);
            return;
        }
        Integer dueDay = request.getCreditCardDueDay();
        if (dueDay == null || dueDay < 1 || dueDay > 31) {
            throw new IllegalArgumentException("Informe o dia de vencimento do cartão (1 a 31).");
        }
        int closingDays = request.getCreditCardClosingDaysBeforeDue() != null
                ? request.getCreditCardClosingDaysBeforeDue()
                : 10;
        if (closingDays < 0 || closingDays > 30) {
            throw new IllegalArgumentException("Dias antes do vencimento deve estar entre 0 e 30.");
        }
        LocalDate nextInvoice = request.getCreditCardNextInvoiceDate() != null
                ? request.getCreditCardNextInvoiceDate()
                : defaultNextInvoiceDate(dueDay, LocalDate.now());
        entity.setCreditCardDueDay(dueDay.shortValue());
        entity.setCreditCardNextInvoiceDate(nextInvoice);
        entity.setCreditCardClosingDaysBeforeDue((short) closingDays);
    }

    static LocalDate defaultNextInvoiceDate(int dueDay, LocalDate reference) {
        LocalDate thisMonth = withDueDay(reference, dueDay);
        if (!thisMonth.isBefore(reference)) {
            return thisMonth;
        }
        LocalDate nextMonth = reference.plusMonths(1);
        return withDueDay(nextMonth, dueDay);
    }

    static LocalDate closingDate(LocalDate nextInvoiceDate, int closingDaysBeforeDue) {
        return nextInvoiceDate.minusDays(closingDaysBeforeDue);
    }

    private static LocalDate withDueDay(LocalDate monthRef, int dueDay) {
        int dom = Math.min(dueDay, monthRef.lengthOfMonth());
        return monthRef.withDayOfMonth(dom);
    }
}
