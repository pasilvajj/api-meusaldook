package com.economy.finance.api.dto;

import com.economy.finance.domain.MoneyKind;
import com.economy.finance.domain.RecurringPeriodicity;
import com.economy.finance.domain.RecurringTransaction;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RecurringTransactionResponse {
    Long id;
    BigDecimal amount;
    MoneyKind kind;
    Long categoryId;
    String categoryName;
    String accountPublicKey;
    String accountName;
    String description;
    Instant startAt;
    RecurringPeriodicity periodicity;
    int everyN;
    Integer maxOccurrences;
    boolean active;
    Instant createdAt;
    boolean showInPayables;

    public static RecurringTransactionResponse from(RecurringTransaction r) {
        return RecurringTransactionResponse.builder()
                .id(r.getId())
                .amount(r.getAmount())
                .kind(r.getKind())
                .categoryId(r.getCategory().getId())
                .categoryName(r.getCategory().getName())
                .accountPublicKey(r.getAccount().getPublicKey())
                .accountName(r.getAccount().getName())
                .description(r.getDescription())
                .startAt(r.getStartAt())
                .periodicity(r.getPeriodicity())
                .everyN(r.getEveryN())
                .maxOccurrences(r.getMaxOccurrences())
                .active(r.isActive())
                .createdAt(r.getCreatedAt())
                .showInPayables(r.isShowInPayables())
                .build();
    }
}
