package com.economy.finance.api.dto;

import com.economy.finance.domain.FinanceTransaction;
import com.economy.finance.domain.MoneyKind;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TransactionResponse {
    Long id;
    BigDecimal amount;
    MoneyKind kind;
    Long categoryId;
    String categoryName;
    String accountPublicKey;
    String accountName;
    String description;
    Instant occurredAt;
    Instant createdAt;
    String installmentGroupId;
    Boolean projected;
    Long recurringId;
    Long sourceTransactionId;
    Integer occurrenceIndex;
    Boolean showInPayables;
    Instant paidAt;

    public static TransactionResponse from(FinanceTransaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .amount(t.getAmount())
                .kind(t.getKind())
                .categoryId(t.getCategory().getId())
                .categoryName(t.getCategory().getName())
                .accountPublicKey(t.getAccount().getPublicKey())
                .accountName(t.getAccount().getName())
                .description(t.getDescription())
                .occurredAt(t.getOccurredAt())
                .createdAt(t.getCreatedAt())
                .installmentGroupId(t.getInstallmentGroupId())
                .projected(false)
                .recurringId(t.getRecurring() != null ? t.getRecurring().getId() : null)
                .showInPayables(t.isShowInPayables())
                .paidAt(t.getPaidAt())
                .build();
    }
}
