package com.economy.finance.api;

import com.economy.finance.api.dto.BudgetGoalMonthResponse;
import com.economy.finance.api.dto.BulkBudgetGoalRequest;
import com.economy.finance.domain.MoneyKind;
import com.economy.finance.service.BudgetGoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/budget-goals")
@RequiredArgsConstructor
public class BudgetGoalController {

    private final BudgetGoalService budgetGoalService;

    @GetMapping
    public BudgetGoalMonthResponse get(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam MoneyKind kind) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Mês inválido");
        }
        return budgetGoalService.getMonth(year, month, kind);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void put(@Valid @RequestBody BulkBudgetGoalRequest request) {
        budgetGoalService.saveBulk(request);
    }
}
