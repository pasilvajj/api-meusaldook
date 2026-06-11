package com.economy.finance.service;

import com.economy.finance.persistence.AppUserRepository;
import com.economy.finance.security.FinancePrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final AppUserRepository appUserRepository;

    public Long requireUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof FinancePrincipal principal) {
            return principal.getUserId();
        }
        String email = auth != null ? auth.getName() : null;
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Sessão inválida");
        }
        return appUserRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalStateException("Sessão inválida"))
                .getId();
    }
}
