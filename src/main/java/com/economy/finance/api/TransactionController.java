package com.economy.finance.api;

import com.economy.finance.api.dto.MarkOccurrencePaidRequest;
import com.economy.finance.api.dto.TransactionRequest;
import com.economy.finance.api.dto.TransactionResponse;
import com.economy.finance.domain.MoneyKind;
import com.economy.finance.service.PaymentService;
import com.economy.finance.service.TransactionService;
import jakarta.validation.Valid;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final PaymentService paymentService;

    @GetMapping
    public Page<TransactionResponse> list(
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) MoneyKind kind,
            @RequestParam(required = false) String accountPublicKey,
            @RequestParam(defaultValue = "false") boolean includeProjected,
            @RequestParam(defaultValue = "false") boolean excludeCreditCards,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return transactionService.list(
                from, to, categoryId, kind, accountPublicKey, includeProjected, excludeCreditCards, pageable);
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@PathVariable Long id) {
        return transactionService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@Valid @RequestBody TransactionRequest request) {
        return transactionService.create(request);
    }

    @PatchMapping("/{id}")
    public TransactionResponse patch(@PathVariable Long id, @Valid @RequestBody TransactionRequest request) {
        return transactionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        transactionService.delete(id);
    }

    @PostMapping("/{id}/mark-paid")
    public TransactionResponse markPaid(@PathVariable Long id) {
        return paymentService.markTransactionPaid(id);
    }

    @PostMapping("/occurrences/mark-paid")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markOccurrencePaid(@Valid @RequestBody MarkOccurrencePaidRequest request) {
        paymentService.markOccurrencePaid(request);
    }
}
