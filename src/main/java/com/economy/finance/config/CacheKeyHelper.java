package com.economy.finance.config;

import com.economy.finance.domain.MoneyKind;
import com.economy.finance.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CacheKeyHelper {

    private final CurrentUserService currentUserService;

    public String monthlySummary(int year, int month, String accountPublicKey) {
        return currentUserService.requireUserId()
                + ":"
                + year
                + ":"
                + month
                + ":"
                + accountKey(accountPublicKey);
    }

    public String budgetGoals(int year, int month, MoneyKind kind) {
        return currentUserService.requireUserId() + ":" + year + ":" + month + ":" + kind.name();
    }

    public String account(String publicKey) {
        return currentUserService.requireUserId() + ":" + publicKey.trim();
    }

    public String dashboard(int year, int month, String accountPublicKey) {
        return currentUserService.requireUserId()
                + ":"
                + year
                + ":"
                + month
                + ":"
                + accountKey(accountPublicKey);
    }

    public String userCategories() {
        return currentUserService.requireUserId() + ":categories";
    }

    public String userAccountList() {
        return currentUserService.requireUserId() + ":accountList";
    }

    private static String accountKey(String accountPublicKey) {
        if (accountPublicKey == null || accountPublicKey.isBlank()) {
            return "principal";
        }
        return accountPublicKey.trim();
    }
}
