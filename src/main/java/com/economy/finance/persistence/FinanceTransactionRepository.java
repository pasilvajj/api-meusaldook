package com.economy.finance.persistence;

import com.economy.finance.domain.FinanceTransaction;
import com.economy.finance.domain.MoneyKind;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinanceTransactionRepository
        extends JpaRepository<FinanceTransaction, Long>, JpaSpecificationExecutor<FinanceTransaction> {

    @EntityGraph(attributePaths = {"category", "account", "recurring"})
    Optional<FinanceTransaction> findByIdAndOwner_Id(Long id, Long ownerId);

    @Override
    @EntityGraph(attributePaths = {"category", "account"})
    Page<FinanceTransaction> findAll(Specification<FinanceTransaction> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"category", "account"})
    List<FinanceTransaction> findAll(Specification<FinanceTransaction> spec, Sort sort);

    @Query(
            "SELECT t.kind, SUM(t.amount) FROM FinanceTransaction t JOIN t.account a "
                    + "WHERE t.owner.id = :userId AND t.occurredAt >= :from AND t.occurredAt < :to "
                    + "AND ("
                    + ":accountPk IS NULL "
                    + "OR a.publicKey = :accountPk "
                    + "OR ("
                    + ":accountPk = 'principal' "
                    + "AND a.accountType = com.economy.finance.domain.AccountType.CREDIT_CARD "
                    + "AND a.considerBalanceMode = com.economy.finance.domain.ConsiderBalanceMode.PENDING "
                    + "AND t.kind = com.economy.finance.domain.MoneyKind.EXPENSE"
                    + ")"
                    + ") "
                    + "GROUP BY t.kind")
    List<Object[]> sumByKindForPeriod(
            @Param("userId") Long userId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("accountPk") String accountPublicKey);

    @Query(
            "SELECT c.name, SUM(t.amount) FROM FinanceTransaction t JOIN t.category c JOIN t.account a "
                    + "WHERE t.owner.id = :userId AND t.occurredAt >= :from AND t.occurredAt < :to "
                    + "AND t.kind = :kind "
                    + "AND ("
                    + ":accountPk IS NULL "
                    + "OR a.publicKey = :accountPk "
                    + "OR ("
                    + ":accountPk = 'principal' "
                    + "AND a.accountType = com.economy.finance.domain.AccountType.CREDIT_CARD "
                    + "AND a.considerBalanceMode = com.economy.finance.domain.ConsiderBalanceMode.PENDING "
                    + "AND t.kind = com.economy.finance.domain.MoneyKind.EXPENSE"
                    + ")"
                    + ") "
                    + "GROUP BY c.id, c.name ORDER BY SUM(t.amount) DESC")
    List<Object[]> sumByCategoryForPeriod(
            @Param("userId") Long userId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("accountPk") String accountPublicKey,
            @Param("kind") MoneyKind kind);

    void deleteByOwner_IdAndInstallmentGroupId(Long ownerId, String installmentGroupId);

    @EntityGraph(attributePaths = {"category", "account"})
    List<FinanceTransaction> findByOwner_IdAndCategory_IdAndAccount_PublicKeyAndKind(
            Long ownerId, Long categoryId, String accountPublicKey, MoneyKind kind);

    @EntityGraph(attributePaths = {"category", "account"})
    List<FinanceTransaction> findByOwner_IdAndRecurring_Id(Long ownerId, Long recurringId);

    @EntityGraph(attributePaths = {"category", "account"})
    @Query(
            "SELECT t FROM FinanceTransaction t WHERE t.owner.id = :userId AND t.description LIKE '%[Fixa:%'")
    List<FinanceTransaction> findFixedExpenseAnchors(@Param("userId") Long userId);
}
