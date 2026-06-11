package com.economy.finance.api.dto;

import com.economy.finance.domain.MoneyKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotNull
    private MoneyKind kind;

    private Long parentId;
}
