package com.economy.finance.persistence;

import com.economy.finance.domain.BudgetGoal;
import com.economy.finance.domain.MoneyKind;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BudgetGoalRepository extends JpaRepository<BudgetGoal, Long> {

    @Query(
            "SELECT bg FROM BudgetGoal bg JOIN FETCH bg.category WHERE bg.owner.id = :ownerId AND bg.kind = :kind AND "
                    + "((bg.goalYear = :y1 AND bg.goalMonth = :m1) OR "
                    + "(bg.goalYear = :y2 AND bg.goalMonth = :m2) OR "
                    + "(bg.goalYear = :y3 AND bg.goalMonth = :m3))")
    List<BudgetGoal> findForMonthWindows(
            @Param("ownerId") Long ownerId,
            @Param("kind") MoneyKind kind,
            @Param("y1") int year1,
            @Param("m1") int month1,
            @Param("y2") int year2,
            @Param("m2") int month2,
            @Param("y3") int year3,
            @Param("m3") int month3);

    List<BudgetGoal> findByOwner_IdAndGoalYearAndGoalMonthAndKind(
            Long ownerId, int goalYear, int goalMonth, MoneyKind kind);

    Optional<BudgetGoal> findByOwner_IdAndCategory_IdAndGoalYearAndGoalMonthAndKind(
            Long ownerId, Long categoryId, int goalYear, int goalMonth, MoneyKind kind);

    void deleteByOwner_IdAndGoalYearAndGoalMonthAndKind(Long ownerId, int goalYear, int goalMonth, MoneyKind kind);
}
