package com.economy.finance.api.dto;

import com.economy.finance.domain.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PersonalInfoPatchRequest(
        @NotBlank @Size(max = 255) String fullName,
        @NotNull Gender gender,
        @Size(max = 14) String cpf,
        LocalDate birthDate) {}
