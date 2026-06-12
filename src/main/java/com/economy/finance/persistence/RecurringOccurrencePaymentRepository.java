package com.economy.finance.persistence;

import com.economy.finance.domain.RecurringOccurrencePayment;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecurringOccurrencePaymentRepository extends JpaRepository<RecurringOccurrencePayment, Long> {

    @EntityGraph(attributePaths = {"recurring", "legacyTransaction"})
    @Query(
            "SELECT p FROM RecurringOccurrencePayment p WHERE p.owner.id = :userId "
                    + "AND p.occurrenceDate >= :from AND p.occurrenceDate < :to")
    List<RecurringOccurrencePayment> findByOwner_IdAndOccurrenceDateInRange(
            @Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    Optional<RecurringOccurrencePayment> findByOwner_IdAndRecurring_IdAndOccurrenceDate(
            Long ownerId, Long recurringId, LocalDate occurrenceDate);

    Optional<RecurringOccurrencePayment> findByOwner_IdAndLegacyTransaction_IdAndOccurrenceDate(
            Long ownerId, Long legacyTransactionId, LocalDate occurrenceDate);

    @EntityGraph(attributePaths = {"materializedTransaction", "recurring", "legacyTransaction"})
    List<RecurringOccurrencePayment> findByOwner_IdAndRecurring_Id(Long ownerId, Long recurringId);

    @EntityGraph(attributePaths = {"materializedTransaction", "legacyTransaction"})
    List<RecurringOccurrencePayment> findByOwner_IdAndLegacyTransaction_Id(Long ownerId, Long legacyTransactionId);

    @EntityGraph(attributePaths = {"materializedTransaction", "recurring", "legacyTransaction"})
    Optional<RecurringOccurrencePayment> findByOwner_IdAndMaterializedTransaction_Id(
            Long ownerId, Long materializedTransactionId);
}
