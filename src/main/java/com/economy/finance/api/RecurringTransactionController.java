package com.economy.finance.api;

import com.economy.finance.api.dto.RecurringTransactionRequest;
import com.economy.finance.api.dto.RecurringTransactionResponse;
import com.economy.finance.domain.MoneyKind;
import com.economy.finance.service.RecurringTransactionService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recurring-transactions")
@RequiredArgsConstructor
public class RecurringTransactionController {

    private final RecurringTransactionService recurringTransactionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecurringTransactionResponse create(@Valid @RequestBody RecurringTransactionRequest request) {
        return recurringTransactionService.create(request);
    }

    @GetMapping
    public List<RecurringTransactionResponse> list(
            @RequestParam(required = false) String accountPublicKey,
            @RequestParam(required = false) MoneyKind kind) {
        return recurringTransactionService.list(accountPublicKey, kind);
    }

    @GetMapping("/{id}")
    public RecurringTransactionResponse get(@PathVariable Long id) {
        return recurringTransactionService.get(id);
    }

    @PatchMapping("/{id}")
    public RecurringTransactionResponse update(
            @PathVariable Long id, @Valid @RequestBody RecurringTransactionRequest request) {
        return recurringTransactionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        recurringTransactionService.delete(id);
    }
}
