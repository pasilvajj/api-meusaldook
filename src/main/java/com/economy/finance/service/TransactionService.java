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
import com.economy.finance.domain.RecurringOccurrencePayment;
import com.economy.finance.persistence.FinanceTransactionRepository;
import com.economy.finance.persistence.FinanceTransactionSpecs;
import com.economy.finance.persistence.RecurringOccurrencePaymentRepository;
import com.economy.finance.persistence.UserAccountRepository;
import com.economy.finance.service.InstallmentParser.InstallmentMeta;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
    private final RecurringProjectionService recurringProjectionService;
    private final RecurringTransactionService recurringTransactionService;
    private final RecurringOccurrencePaymentRepository occurrencePaymentRepository;

    @Transactional(readOnly = true)
    public Page<TransactionResponse> list(
            Instant from,
            Instant to,
            Long categoryId,
            MoneyKind kind,
            String accountPublicKey,
            boolean includeProjected,
            Pageable pageable) {
        Long userId = currentUserService.requireUserId();
        String accountKey = blankToNull(accountPublicKey);
        Specification<FinanceTransaction> spec =
                FinanceTransactionSpecs.forUser(userId, from, to, categoryId, kind, accountKey);

        if (!includeProjected || from == null || to == null) {
            return transactionRepository.findAll(spec, pageable).map(TransactionResponse::from);
        }

        List<TransactionResponse> persisted =
                transactionRepository.findAll(spec, pageable.getSort()).stream()
                        .map(TransactionResponse::from)
                        .toList();
        List<TransactionResponse> projected =
                recurringProjectionService.project(userId, from, to, categoryId, kind, accountKey);

        List<TransactionResponse> merged = new ArrayList<>(persisted.size() + projected.size());
        merged.addAll(persisted);
        merged.addAll(projected);
        merged.sort(
                Comparator.comparing(TransactionResponse::getOccurredAt)
                        .reversed()
                        .thenComparing(TransactionResponse::getId, Comparator.reverseOrder()));

        int total = merged.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), total);
        List<TransactionResponse> pageContent =
                start < total ? merged.subList(start, end) : List.of();
        return new PageImpl<>(pageContent, pageable, total);
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
                        .installmentGroupId(blankToNull(request.getInstallmentGroupId()))
                        .showInPayables(Boolean.TRUE.equals(request.getShowInPayables()))
                        .paidAt(
                                Boolean.TRUE.equals(request.getMarkAsPaid())
                                        ? Instant.now()
                                        : null)
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
        if (request.getShowInPayables() != null) {
            entity.setShowInPayables(request.getShowInPayables());
        }
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

        if (entity.getRecurring() != null) {
            recurringTransactionService.delete(entity.getRecurring().getId());
            userCacheEvictor.evictUser(userId);
            return;
        }

        Optional<RecurringOccurrencePayment> materializedPayment =
                occurrencePaymentRepository.findByOwner_IdAndMaterializedTransaction_Id(userId, id);
        if (materializedPayment.isPresent()) {
            RecurringOccurrencePayment payment = materializedPayment.get();
            if (payment.getRecurring() != null) {
                recurringTransactionService.delete(payment.getRecurring().getId());
                userCacheEvictor.evictUser(userId);
                return;
            }
            if (payment.getLegacyTransaction() != null) {
                deleteLegacyFixedExpense(userId, payment.getLegacyTransaction());
                userCacheEvictor.evictUser(userId);
                return;
            }
        }

        String groupId = entity.getInstallmentGroupId();
        if (groupId != null && !groupId.isBlank()) {
            transactionRepository.deleteByOwner_IdAndInstallmentGroupId(userId, groupId);
            userCacheEvictor.evictUser(userId);
            return;
        }

        if (FixedExpenseParser.parse(entity.getDescription()).isPresent()) {
            deleteLegacyFixedExpense(userId, entity);
            userCacheEvictor.evictUser(userId);
            return;
        }

        InstallmentParser.parse(entity.getDescription())
                .ifPresentOrElse(
                        meta -> transactionRepository.deleteAll(findInstallmentSiblings(userId, entity, meta)),
                        () -> transactionRepository.delete(entity));
        userCacheEvictor.evictUser(userId);
    }

    private void deleteLegacyFixedExpense(Long userId, FinanceTransaction anchor) {
        List<RecurringOccurrencePayment> payments =
                occurrencePaymentRepository.findByOwner_IdAndLegacyTransaction_Id(userId, anchor.getId());
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
        transactionRepository.delete(anchor);
    }

    private List<FinanceTransaction> findInstallmentSiblings(
            Long userId, FinanceTransaction entity, InstallmentMeta meta) {
        return transactionRepository
                .findByOwner_IdAndCategory_IdAndAccount_PublicKeyAndKind(
                        userId,
                        entity.getCategory().getId(),
                        entity.getAccount().getPublicKey(),
                        entity.getKind())
                .stream()
                .filter(candidate -> InstallmentParser.sameInstallmentGroup(meta, candidate.getDescription()))
                .toList();
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
