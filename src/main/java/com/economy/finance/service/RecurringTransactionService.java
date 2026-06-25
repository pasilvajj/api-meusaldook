package com.economy.finance.service;

import com.economy.finance.api.dto.RecurringTransactionRequest;
import com.economy.finance.api.dto.RecurringTransactionResponse;
import com.economy.finance.api.exception.ResourceNotFoundException;
import com.economy.finance.config.UserCacheEvictor;
import com.economy.finance.domain.AppUser;
import com.economy.finance.domain.Category;
import com.economy.finance.domain.MoneyKind;
import com.economy.finance.domain.RecurringTransaction;
import com.economy.finance.domain.UserAccount;
import com.economy.finance.persistence.AppUserRepository;
import com.economy.finance.persistence.CategoryRepository;
import com.economy.finance.domain.FinanceTransaction;
import com.economy.finance.domain.RecurringOccurrencePayment;
import com.economy.finance.persistence.FinanceTransactionRepository;
import com.economy.finance.persistence.RecurringOccurrencePaymentRepository;
import com.economy.finance.persistence.RecurringTransactionRepository;
import com.economy.finance.persistence.UserAccountRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringRepository;
    private final RecurringOccurrencePaymentRepository occurrencePaymentRepository;
    private final FinanceTransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final AppUserRepository appUserRepository;
    private final UserAccountRepository userAccountRepository;
    private final CurrentUserService currentUserService;
    private final UserCacheEvictor userCacheEvictor;

    @Transactional
    public RecurringTransactionResponse create(RecurringTransactionRequest request) {
        Long userId = currentUserService.requireUserId();
        AppUser owner = appUserRepository.getReferenceById(userId);
        Category category = resolveCategory(userId, request.getCategoryId(), request.getKind());
        UserAccount account = resolveAccount(userId, request.getAccountPublicKey());
        RecurringTransaction entity =
                RecurringTransaction.builder()
                        .owner(owner)
                        .category(category)
                        .account(account)
                        .amount(request.getAmount())
                        .kind(request.getKind())
                        .description(request.getDescription())
                        .startAt(request.getStartAt())
                        .periodicity(request.getPeriodicity())
                        .everyN(Math.max(1, request.getEveryN()))
                        .maxOccurrences(request.getMaxOccurrences())
                        .active(true)
                        .createdAt(Instant.now())
                        .showInPayables(Boolean.TRUE.equals(request.getShowInPayables()))
                        .build();
        RecurringTransactionResponse created =
                RecurringTransactionResponse.from(recurringRepository.save(entity));
        userCacheEvictor.evictUser(userId);
        return created;
    }

    @Transactional(readOnly = true)
    public RecurringTransactionResponse get(Long id) {
        Long userId = currentUserService.requireUserId();
        RecurringTransaction entity =
                recurringRepository
                        .findByIdAndOwner_Id(id, userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Despesa fixa não encontrada"));
        return RecurringTransactionResponse.from(entity);
    }

    @Transactional(readOnly = true)
    public List<RecurringTransactionResponse> list(String accountPublicKey, MoneyKind kind) {
        Long userId = currentUserService.requireUserId();
        String key =
                accountPublicKey == null || accountPublicKey.isBlank() ? null : accountPublicKey.trim();
        if (key != null && kind != null) {
            return recurringRepository
                    .findByOwner_IdAndAccount_PublicKeyAndKindAndActiveTrue(userId, key, kind)
                    .stream()
                    .map(RecurringTransactionResponse::from)
                    .toList();
        }
        return recurringRepository.findByOwner_IdAndActiveTrue(userId).stream()
                .filter(
                        r ->
                                (key == null || key.equals(r.getAccount().getPublicKey()))
                                        && (kind == null || kind == r.getKind()))
                .map(RecurringTransactionResponse::from)
                .toList();
    }

    @Transactional
    public RecurringTransactionResponse update(Long id, RecurringTransactionRequest request) {
        Long userId = currentUserService.requireUserId();
        RecurringTransaction entity =
                recurringRepository
                        .findByIdAndOwner_Id(id, userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Despesa fixa não encontrada"));
        Category category = resolveCategory(userId, request.getCategoryId(), request.getKind());
        UserAccount account = resolveAccount(userId, request.getAccountPublicKey());
        entity.setAmount(request.getAmount());
        entity.setKind(request.getKind());
        entity.setCategory(category);
        entity.setAccount(account);
        entity.setDescription(request.getDescription());
        entity.setStartAt(request.getStartAt());
        entity.setPeriodicity(request.getPeriodicity());
        entity.setEveryN(Math.max(1, request.getEveryN()));
        entity.setMaxOccurrences(request.getMaxOccurrences());
        if (request.getShowInPayables() != null) {
            entity.setShowInPayables(request.getShowInPayables());
        }
        RecurringTransactionResponse updated = RecurringTransactionResponse.from(entity);
        userCacheEvictor.evictUser(userId);
        return updated;
    }

    @Transactional
    public void delete(Long id) {
        Long userId = currentUserService.requireUserId();
        RecurringTransaction entity =
                recurringRepository
                        .findByIdAndOwner_Id(id, userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Despesa fixa não encontrada"));
        deleteRecurringCascade(userId, entity);
        userCacheEvictor.evictUser(userId);
    }

    private void deleteRecurringCascade(Long userId, RecurringTransaction recurring) {
        Long recurringId = recurring.getId();
        List<RecurringOccurrencePayment> payments =
                occurrencePaymentRepository.findByOwner_IdAndRecurring_Id(userId, recurringId);

        List<FinanceTransaction> materialized = new ArrayList<>();
        for (RecurringOccurrencePayment payment : payments) {
            if (payment.getMaterializedTransaction() != null) {
                materialized.add(payment.getMaterializedTransaction());
            }
        }

        occurrencePaymentRepository.deleteAll(payments);
        occurrencePaymentRepository.flush();

        if (!materialized.isEmpty()) {
            transactionRepository.deleteAll(materialized);
        }

        List<FinanceTransaction> linked =
                transactionRepository.findByOwner_IdAndRecurring_Id(userId, recurringId);
        if (!linked.isEmpty()) {
            transactionRepository.deleteAll(linked);
        }

        recurringRepository.delete(recurring);
    }

    private Category resolveCategory(Long userId, Long categoryId, com.economy.finance.domain.MoneyKind kind) {
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
}
