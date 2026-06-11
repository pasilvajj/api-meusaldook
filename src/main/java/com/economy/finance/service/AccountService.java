package com.economy.finance.service;

import com.economy.finance.api.dto.AccountResponse;
import com.economy.finance.api.dto.AccountWriteRequest;
import com.economy.finance.api.exception.ConflictException;
import com.economy.finance.api.exception.ResourceNotFoundException;
import com.economy.finance.domain.AccountType;
import com.economy.finance.domain.AppUser;
import com.economy.finance.domain.ConsiderBalanceMode;
import com.economy.finance.domain.SaldoCreditorDebtor;
import com.economy.finance.domain.UserAccount;
import com.economy.finance.config.UserCacheEvictor;
import com.economy.finance.persistence.AppUserRepository;
import com.economy.finance.persistence.UserAccountRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserAccountRepository userAccountRepository;
    private final AppUserRepository appUserRepository;
    private final CurrentUserService currentUserService;
    private final UserCacheEvictor userCacheEvictor;

    @Transactional(readOnly = true)
    @Cacheable(value = "accountList", key = "@cacheKeyHelper.userAccountList()")
    public List<AccountResponse> listAll() {
        Long userId = currentUserService.requireUserId();
        return userAccountRepository.findByOwner_IdOrderByAccountTypeAscNameAsc(userId).stream()
                .map(AccountResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "accounts", key = "@cacheKeyHelper.account(#publicKey)")
    public AccountResponse getByPublicKey(String publicKey) {
        Long userId = currentUserService.requireUserId();
        UserAccount a =
                userAccountRepository
                        .findByOwner_IdAndPublicKey(userId, publicKey.trim())
                        .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada"));
        return AccountResponse.from(a);
    }

    @Transactional(readOnly = true)
    public AccountResponse get(Long id) {
        Long userId = currentUserService.requireUserId();
        UserAccount a =
                userAccountRepository
                        .findByIdAndOwner_Id(id, userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada"));
        return AccountResponse.from(a);
    }

    @Transactional
    public AccountResponse create(AccountWriteRequest request) {
        Long userId = currentUserService.requireUserId();
        AppUser owner = appUserRepository.getReferenceById(userId);
        String key =
                request.getPublicKey() != null && !request.getPublicKey().isBlank()
                        ? request.getPublicKey().trim()
                        : UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        if (userAccountRepository.existsByOwner_IdAndPublicKey(userId, key)) {
            throw new ConflictException("Já existe uma conta com esta chave");
        }
        Instant now = Instant.now();
        UserAccount entity =
                UserAccount.builder()
                        .owner(owner)
                        .publicKey(key)
                        .name(request.getName().trim())
                        .accountType(request.getAccountType())
                        .currency(request.getCurrency().trim().toUpperCase())
                        .active(Boolean.TRUE.equals(request.getActive()))
                        .initialBalanceDate(request.getInitialBalanceDate())
                        .initialBalanceAmount(request.getInitialBalanceAmount().abs())
                        .saldoCreditorDebtor(request.getSaldoCreditorDebtor())
                        .considerBalanceMode(request.getConsiderBalanceMode())
                        .notes(trimToNull(request.getNotes()))
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
        AccountResponse created = AccountResponse.from(userAccountRepository.save(entity));
        userCacheEvictor.evictUser(userId);
        return created;
    }

    @Transactional
    public AccountResponse update(Long id, AccountWriteRequest request) {
        Long userId = currentUserService.requireUserId();
        UserAccount entity =
                userAccountRepository
                        .findByIdAndOwner_Id(id, userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada"));
        if (request.getPublicKey() != null
                && !request.getPublicKey().isBlank()
                && !entity.getPublicKey().equals(request.getPublicKey().trim())) {
            if (userAccountRepository.existsByOwner_IdAndPublicKey(userId, request.getPublicKey().trim())) {
                throw new ConflictException("Já existe uma conta com esta chave");
            }
            entity.setPublicKey(request.getPublicKey().trim());
        }
        entity.setName(request.getName().trim());
        entity.setAccountType(request.getAccountType());
        entity.setCurrency(request.getCurrency().trim().toUpperCase());
        entity.setActive(Boolean.TRUE.equals(request.getActive()));
        entity.setInitialBalanceDate(request.getInitialBalanceDate());
        entity.setInitialBalanceAmount(request.getInitialBalanceAmount().abs());
        entity.setSaldoCreditorDebtor(request.getSaldoCreditorDebtor());
        entity.setConsiderBalanceMode(request.getConsiderBalanceMode());
        entity.setNotes(trimToNull(request.getNotes()));
        entity.setUpdatedAt(Instant.now());
        AccountResponse updated = AccountResponse.from(userAccountRepository.save(entity));
        userCacheEvictor.evictUser(userId);
        return updated;
    }

    @Transactional
    public void seedPrincipalIfMissing(Long userId) {
        if (userAccountRepository.existsByOwner_IdAndPublicKey(userId, "principal")) {
            return;
        }
        AppUser owner = appUserRepository.getReferenceById(userId);
        Instant now = Instant.now();
        UserAccount entity =
                UserAccount.builder()
                        .owner(owner)
                        .publicKey("principal")
                        .name("Conta principal")
                        .accountType(AccountType.CHECKING)
                        .currency("BRL")
                        .active(true)
                        .initialBalanceDate(java.time.LocalDate.now())
                        .initialBalanceAmount(BigDecimal.ZERO)
                        .saldoCreditorDebtor(SaldoCreditorDebtor.CREDITOR)
                        .considerBalanceMode(ConsiderBalanceMode.IMMEDIATE)
                        .notes(null)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
        userAccountRepository.save(entity);
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
