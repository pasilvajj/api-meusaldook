package com.economy.finance.api.dto;

import com.economy.finance.domain.MoneyKind;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;

@Data
public class TransactionRequest {

    @NotNull
    @DecimalMin(value = "0.0001", inclusive = true)
    private BigDecimal amount;

    @NotNull
    private MoneyKind kind;

    @NotNull
    private Long categoryId;

    /** Chave pública da conta (ex.: `principal`). Se omitido, usa-se `principal`. */
    @Size(max = 64)
    private String accountPublicKey;

    @Size(max = 1024)
    private String description;

    @NotNull
    private Instant occurredAt;

    /** Agrupa parcelas da mesma despesa parcelada (UUID). */
    @Size(max = 36)
    private String installmentGroupId;

    private Boolean showInPayables;

  /** Ao criar, marca a despesa como paga (`paid_at` = agora). */
  private Boolean markAsPaid;

  /** Ao editar parcela: propaga categoria e base da descrição às demais parcelas do grupo. */
  private Boolean applyToInstallmentGroup;
}
