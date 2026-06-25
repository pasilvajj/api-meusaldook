package com.economy.finance.service;

import com.economy.finance.api.dto.MarkOccurrencePaidRequest;
import com.economy.finance.api.dto.TransactionResponse;
import com.economy.finance.api.exception.ResourceNotFoundException;
import com.economy.finance.config.UserCacheEvictor;
import com.economy.finance.domain.AppUser;
import com.economy.finance.domain.Category;
import com.economy.finance.domain.FinanceTransaction;
import com.economy.finance.domain.MoneyKind;
import com.economy.finance.domain.RecurringOccurrencePayment;
import com.economy.finance.domain.RecurringTransaction;
import com.economy.finance.domain.UserAccount;
import com.economy.finance.persistence.AppUserRepository;
import com.economy.finance.persistence.FinanceTransactionRepository;
import com.economy.finance.persistence.RecurringOccurrencePaymentRepository;
import com.economy.finance.persistence.RecurringTransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final FinanceTransactionRepository transactionRepository;
    private final RecurringTransactionRepository recurringRepository;
    private final RecurringOccurrencePaymentRepository occurrencePaymentRepository;
    private final AppUserRepository appUserRepository;
    private final CurrentUserService currentUserService;
    private final UserCacheEvictor userCacheEvictor;

    @Transactional
    public TransactionResponse markTransactionPaid(Long id) {
        Long userId = currentUserService.requireUserId();
        FinanceTransaction entity =
                transactionRepository
                        .findByIdAndOwner_Id(id, userId)
                        .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada"));
        if (entity.getPaidAt() == null) {
            entity.setPaidAt(Instant.now());
        }
        userCacheEvictor.evictUser(userId);
        return TransactionResponse.from(entity);
    }

    @Transactional
    public void markOccurrencePaid(MarkOccurrencePaidRequest request) {
        Long userId = currentUserService.requireUserId();
        LocalDate occurrenceDate = toUtcDate(request.getOccurredAt());
        Instant paidAt = Instant.now();

        if (request.getRecurringId() != null) {
            RecurringTransaction recurring =
                    recurringRepository
                            .findByIdAndOwner_Id(request.getRecurringId(), userId)
                            .orElseThrow(() -> new ResourceNotFoundException("Regra recorrente não encontrada"));
            upsertOccurrencePayment(userId, recurring, null, occurrenceDate, paidAt);
        } else if (request.getLegacyTransactionId() != null) {
            FinanceTransaction legacy =
                    transactionRepository
                            .findByIdAndOwner_Id(request.getLegacyTransactionId(), userId)
                            .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada"));
            upsertOccurrencePayment(userId, null, legacy, occurrenceDate, paidAt);
        } else {
            throw new IllegalArgumentException("Informe recurringId ou legacyTransactionId");
        }

        userCacheEvictor.evictUser(userId);
    }

    private void upsertOccurrencePayment(
            Long userId,
            RecurringTransaction recurring,
            FinanceTransaction legacy,
            LocalDate occurrenceDate,
            Instant paidAt) {
        Optional<RecurringOccurrencePayment> existing =
                recurring != null
                        ? occurrencePaymentRepository.findByOwner_IdAndRecurring_IdAndOccurrenceDate(
                                userId, recurring.getId(), occurrenceDate)
                        : occurrencePaymentRepository.findByOwner_IdAndLegacyTransaction_IdAndOccurrenceDate(
                                userId, legacy.getId(), occurrenceDate);

        RecurringOccurrencePayment payment;
        if (existing.isPresent()) {
            payment = existing.get();
            payment.setPaidAt(paidAt);
        } else {
            AppUser owner = appUserRepository.getReferenceById(userId);
            payment =
                    occurrencePaymentRepository.save(
                            RecurringOccurrencePayment.builder()
                                    .owner(owner)
                                    .recurring(recurring)
                                    .legacyTransaction(legacy)
                                    .occurrenceDate(occurrenceDate)
                                    .paidAt(paidAt)
                                    .build());
        }

        FinanceTransaction materialized = payment.getMaterializedTransaction();
        if (materialized == null) {
            materialized = materializeOccurrence(userId, recurring, legacy, occurrenceDate, paidAt);
            payment.setMaterializedTransaction(materialized);
            occurrencePaymentRepository.save(payment);
        } else if (materialized.getPaidAt() == null) {
            materialized.setPaidAt(paidAt);
        }
    }

    private FinanceTransaction materializeOccurrence(
            Long userId,
            RecurringTransaction recurring,
            FinanceTransaction legacy,
            LocalDate occurrenceDate,
            Instant paidAt) {
        AppUser owner = appUserRepository.getReferenceById(userId);
        Category category;
        UserAccount account;
        BigDecimal amount;
        MoneyKind kind;
        String description;
        Instant anchorAt;

        if (recurring != null) {
            category = recurring.getCategory();
            account = recurring.getAccount();
            amount = recurring.getAmount();
            kind = recurring.getKind();
            description = recurring.getDescription();
            anchorAt = recurring.getStartAt();
        } else {
            category = legacy.getCategory();
            account = legacy.getAccount();
            amount = legacy.getAmount();
            kind = legacy.getKind();
            description = legacy.getDescription();
            anchorAt = legacy.getOccurredAt();
        }

        FinanceTransaction.FinanceTransactionBuilder builder =
                FinanceTransaction.builder()
                        .owner(owner)
                        .category(category)
                        .account(account)
                        .amount(amount)
                        .kind(kind)
                        .description(description)
                        .occurredAt(occurredAtFromAnchor(anchorAt, occurrenceDate))
                        .createdAt(paidAt)
                        .showInPayables(false)
                        .paidAt(paidAt);
        if (recurring != null) {
            builder.recurring(recurring);
        }
        return transactionRepository.save(builder.build());
    }

    static Instant occurredAtFromAnchor(Instant anchorAt, LocalDate occurrenceDate) {
        ZonedDateTime anchor = anchorAt.atZone(ZoneOffset.UTC);
        return ZonedDateTime.of(occurrenceDate, anchor.toLocalTime(), ZoneOffset.UTC).toInstant();
    }

    static LocalDate toUtcDate(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).toLocalDate();
    }
}
