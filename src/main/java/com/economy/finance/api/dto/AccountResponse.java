package com.economy.finance.api.dto;

import com.economy.finance.domain.AccountType;
import com.economy.finance.domain.ConsiderBalanceMode;
import com.economy.finance.domain.SaldoCreditorDebtor;
import com.economy.finance.domain.UserAccount;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AccountResponse {
    Long id;
    String publicKey;
    String name;
    boolean active;
    String statusLabel;
    AccountType accountType;
    String currency;
    LocalDate initialBalanceDate;
    BigDecimal initialBalanceAmount;
    SaldoCreditorDebtor saldoCreditorDebtor;
    ConsiderBalanceMode considerBalanceMode;
    Integer creditCardDueDay;
    LocalDate creditCardNextInvoiceDate;
    Integer creditCardClosingDaysBeforeDue;
    String notes;
    BigDecimal signedInitialBalance;
    Instant createdAt;
    Instant updatedAt;

    public static AccountResponse from(UserAccount a) {
        BigDecimal absAmt = a.getInitialBalanceAmount().abs();
        BigDecimal signed =
                a.getSaldoCreditorDebtor() == SaldoCreditorDebtor.DEBTOR ? absAmt.negate() : absAmt;
        return AccountResponse.builder()
                .id(a.getId())
                .publicKey(a.getPublicKey())
                .name(a.getName())
                .active(a.isActive())
                .statusLabel(a.isActive() ? "Disponível" : "Inativa")
                .accountType(a.getAccountType())
                .currency(a.getCurrency())
                .initialBalanceDate(a.getInitialBalanceDate())
                .initialBalanceAmount(absAmt)
                .saldoCreditorDebtor(a.getSaldoCreditorDebtor())
                .considerBalanceMode(a.getConsiderBalanceMode())
                .creditCardDueDay(toInteger(a.getCreditCardDueDay()))
                .creditCardNextInvoiceDate(a.getCreditCardNextInvoiceDate())
                .creditCardClosingDaysBeforeDue(toInteger(a.getCreditCardClosingDaysBeforeDue()))
                .notes(a.getNotes())
                .signedInitialBalance(signed)
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }

    private static Integer toInteger(Short value) {
        return value == null ? null : value.intValue();
    }
}
