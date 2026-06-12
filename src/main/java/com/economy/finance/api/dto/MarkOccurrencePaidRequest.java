package com.economy.finance.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import lombok.Data;

@Data
public class MarkOccurrencePaidRequest {

    private Long recurringId;

    private Long legacyTransactionId;

    @NotNull
    private Instant occurredAt;
}
