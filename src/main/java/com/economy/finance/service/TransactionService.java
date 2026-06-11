package com.economy.finance.service;

import com.economy.finance.api.dto.TransactionRequest;
import com.economy.finance.api.dto.TransactionResponse;
import com.economy.finance.api.exception.ResourceNotFoundException;
import com.economy.finance.domain.AppUser;
import com.economy.finance.domain.Category;
import com.economy.finance.domain.FinanceTransaction;
import com.economy.finance.domain.MoneyKind;
import com.economy.finance.domain.UserAccount;
import com.economy.finance.config.UserCacheEvictor;
import com.economy.finance.persistence.AppUserRepository;
import com.economy.finance.persistence.CategoryRepository;
import com.economy.finance.persistence.FinanceTransactionRepository;
import com.economy.finance.persistence.FinanceTransactionSpecs;
import com.economy.finance.persistence.UserAccountRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final FinanceTransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final AppUserRepository appUserRepository;
    private final UserAccountRepository userAccountRepository;
    private final CurrentUserService currentUserService;
    private final UserCacheEvictor userCacheEvictor;

    @Transactional(readOnly = true)
    public Page<TransactionResponse> list(
            Instant from,
            Instant to,
            Long categoryId,
            MoneyKind kind,
            String accountPublicKey,
            Pageable pageable) {
        Long userId = currentUserService.requireUserId();
        Specification<FinanceTransaction> spec =
                FinanceTransactionSpecs.forUser(
                        userId, from, to, categoryId, kind, blankToNull(accountPublicKey));
        return transactionRepository.findAll(spec, pageable).map(TransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(Long id) {
        Long userId = currentUserService.requireUserId();
        FinanceTransaction t =
                transactionRepository
                        .findByIdAndOwner_Id(id, userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada"));
        return TransactionResponse.from(t);
    }

    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        Long userId = currentUserService.requireUserId();
        AppUser owner = appUserRepository.getReferenceById(userId);
        Category category = resolveCategory(userId, request.getCategoryId(), request.getKind());
        UserAccount account = resolveAccount(userId, request.getAccountPublicKey());
        FinanceTransaction entity =
                FinanceTransaction.builder()
                        .owner(owner)
                        .category(category)
                        .account(account)
                        .amount(request.getAmount())
                        .kind(request.getKind())
                        .description(request.getDescription())
                        .occurredAt(request.getOccurredAt())
                        .createdAt(Instant.now())
                        .build();
        TransactionResponse created = TransactionResponse.from(transactionRepository.save(entity));
        userCacheEvictor.evictUser(userId);
        return created;
    }

    @Transactional
    public TransactionResponse update(Long id, TransactionRequest request) {
        Long userId = currentUserService.requireUserId();
        FinanceTransaction entity =
                transactionRepository
                        .findByIdAndOwner_Id(id, userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada"));
        Category category = resolveCategory(userId, request.getCategoryId(), request.getKind());
        UserAccount account = resolveAccount(userId, request.getAccountPublicKey());
        entity.setAmount(request.getAmount());
        entity.setKind(request.getKind());
        entity.setCategory(category);
        entity.setAccount(account);
        entity.setDescription(request.getDescription());
        entity.setOccurredAt(request.getOccurredAt());
        TransactionResponse updated = TransactionResponse.from(entity);
        userCacheEvictor.evictUser(userId);
        return updated;
    }

    @Transactional
    public void delete(Long id) {
        Long userId = currentUserService.requireUserId();
        FinanceTransaction entity =
                transactionRepository
                        .findByIdAndOwner_Id(id, userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada"));
        transactionRepository.delete(entity);
        userCacheEvictor.evictUser(userId);
    }

    private Category resolveCategory(Long userId, Long categoryId, MoneyKind kind) {
        Category category =
                categoryRepository
                        .findByIdAndOwner_Id(categoryId, userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada"));
        if (category.getKind() != kind) {
            throw new IllegalArgumentException("O tipo da transação deve coincidir com o tipo da categoria");
        }
        return category;
    }

    private UserAccount resolveAccount(Long userId, String accountPublicKey) {
        String key =
                accountPublicKey == null || accountPublicKey.isBlank()
                        ? "principal"
                        : accountPublicKey.trim();
        return userAccountRepository
                .findByOwner_IdAndPublicKey(userId, key)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada"));
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
