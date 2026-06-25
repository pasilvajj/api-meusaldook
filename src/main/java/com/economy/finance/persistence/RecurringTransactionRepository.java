package com.economy.finance.persistence;

import com.economy.finance.domain.MoneyKind;
import com.economy.finance.domain.RecurringTransaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    @EntityGraph(attributePaths = {"category", "account"})
    List<RecurringTransaction> findByOwner_IdAndActiveTrue(Long ownerId);

    @EntityGraph(attributePaths = {"category", "account"})
    List<RecurringTransaction> findByOwner_IdAndAccount_PublicKeyAndKindAndActiveTrue(
            Long ownerId, String accountPublicKey, MoneyKind kind);

    @EntityGraph(attributePaths = {"category", "account"})
    Optional<RecurringTransaction> findByIdAndOwner_Id(Long id, Long ownerId);
}
