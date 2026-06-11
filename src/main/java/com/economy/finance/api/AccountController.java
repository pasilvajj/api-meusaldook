package com.economy.finance.api;

import com.economy.finance.api.dto.AccountResponse;
import com.economy.finance.api.dto.AccountWriteRequest;
import com.economy.finance.service.AccountService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public List<AccountResponse> list() {
        return accountService.listAll();
    }

    @GetMapping("/by-key/{publicKey}")
    public AccountResponse getByKey(@PathVariable String publicKey) {
        return accountService.getByPublicKey(publicKey);
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable Long id) {
        return accountService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody AccountWriteRequest request) {
        return accountService.create(request);
    }

    @PatchMapping("/{id}")
    public AccountResponse patch(@PathVariable Long id, @Valid @RequestBody AccountWriteRequest request) {
        return accountService.update(id, request);
    }
}
