package com.economy.finance.api.dto;

import com.economy.finance.domain.AccountType;
import com.economy.finance.domain.ConsiderBalanceMode;
import com.economy.finance.domain.SaldoCreditorDebtor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class AccountWriteRequest {

    @Size(max = 64)
    private String publicKey;

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotNull
    private AccountType accountType;

    @NotBlank
    @Size(max = 8)
    private String currency;

    @NotNull
    private Boolean active;

    @NotNull
    private LocalDate initialBalanceDate;

    @NotNull
    private BigDecimal initialBalanceAmount;

    @NotNull
    private SaldoCreditorDebtor saldoCreditorDebtor;

    @NotNull
    private ConsiderBalanceMode considerBalanceMode;

    @Size(max = 2000)
    private String notes;
}
