package com.economy.finance.service;

import com.economy.finance.api.dto.BudgetGoalMonthResponse;
import com.economy.finance.api.dto.BulkBudgetGoalRequest;
import com.economy.finance.domain.AppUser;
import com.economy.finance.domain.BudgetGoal;
import com.economy.finance.domain.Category;
import com.economy.finance.domain.MoneyKind;
import com.economy.finance.config.UserCacheEvictor;
import com.economy.finance.persistence.AppUserRepository;
import com.economy.finance.persistence.BudgetGoalRepository;
import com.economy.finance.persistence.CategoryRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BudgetGoalService {

    private final BudgetGoalRepository budgetGoalRepository;
    private final CategoryRepository categoryRepository;
    private final AppUserRepository appUserRepository;
    private final CurrentUserService currentUserService;
    private final UserCacheEvictor userCacheEvictor;

    @Transactional(readOnly = true)
    @Cacheable(value = "budgetGoals", key = "@cacheKeyHelper.budgetGoals(#year, #month, #kind)")
    public BudgetGoalMonthResponse getMonth(int year, int month, MoneyKind kind) {
        Long userId = currentUserService.requireUserId();
        List<Category> categories = categoryRepository.findByOwner_IdAndKindOrderByNameAsc(userId, kind);
        YearMonth current = YearMonth.of(year, month);
        YearMonth prev = current.minusMonths(1);
        YearMonth prevYearSame = YearMonth.of(year - 1, month);

        Map<String, BigDecimal> amountsByCategoryMonth = new HashMap<>();
        for (BudgetGoal goal :
                budgetGoalRepository.findForMonthWindows(
                        userId,
                        kind,
                        year,
                        month,
                        prev.getYear(),
                        prev.getMonthValue(),
                        prevYearSame.getYear(),
                        prevYearSame.getMonthValue())) {
            amountsByCategoryMonth.put(
                    goalKey(goal.getCategory().getId(), goal.getGoalYear(), goal.getGoalMonth()),
                    goal.getAmount());
        }

        List<BudgetGoalMonthResponse.Row> rows = new ArrayList<>();
        for (Category c : categories) {
            rows.add(
                    BudgetGoalMonthResponse.Row.builder()
                            .categoryId(c.getId())
                            .categoryName(c.getName())
                            .currentGoal(amountFor(amountsByCategoryMonth, c.getId(), year, month))
                            .previousMonthGoal(
                                    amountFor(
                                            amountsByCategoryMonth,
                                            c.getId(),
                                            prev.getYear(),
                                            prev.getMonthValue()))
                            .previousYearSameMonthGoal(
                                    amountFor(
                                            amountsByCategoryMonth,
                                            c.getId(),
                                            prevYearSame.getYear(),
                                            prevYearSame.getMonthValue()))
                            .build());
        }
        return BudgetGoalMonthResponse.builder()
                .year(year)
                .month(month)
                .kind(kind)
                .rows(rows)
                .build();
    }

    private static String goalKey(Long categoryId, int year, int month) {
        return categoryId + ":" + year + ":" + month;
    }

    private static BigDecimal amountFor(Map<String, BigDecimal> amounts, Long categoryId, int year, int month) {
        return amounts.getOrDefault(goalKey(categoryId, year, month), BigDecimal.ZERO);
    }

    @Transactional
    public void saveBulk(BulkBudgetGoalRequest request) {
        Long userId = currentUserService.requireUserId();
        AppUser owner = appUserRepository.getReferenceById(userId);
        int year = request.getYear();
        int month = request.getMonth();
        MoneyKind kind = request.getKind();

        budgetGoalRepository.deleteByOwner_IdAndGoalYearAndGoalMonthAndKind(userId, year, month, kind);
        // O DELETE derivado pode ficar na fila do Hibernate até ao flush; sem isto os INSERT seguintes
        // podem falhar com Duplicate entry na uk_budget_goal no mesmo pedido.
        budgetGoalRepository.flush();

        Instant now = Instant.now();
        Map<Long, BigDecimal> amountsByCategory = new LinkedHashMap<>();
        for (BulkBudgetGoalRequest.Line line : request.getLines()) {
            amountsByCategory.put(line.getCategoryId(), line.getAmount());
        }
        for (Map.Entry<Long, BigDecimal> e : amountsByCategory.entrySet()) {
            if (e.getValue().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Category category =
                    categoryRepository
                            .findByIdAndOwner_Id(e.getKey(), userId)
                            .orElseThrow(() -> new com.economy.finance.api.exception.ResourceNotFoundException("Categoria inválida"));
            if (category.getKind() != kind) {
                throw new IllegalArgumentException("O tipo da meta deve coincidir com o tipo da categoria");
            }
            BudgetGoal entity =
                    BudgetGoal.builder()
                            .owner(owner)
                            .category(category)
                            .goalYear(year)
                            .goalMonth(month)
                            .amount(e.getValue())
                            .kind(kind)
                            .createdAt(now)
                            .updatedAt(now)
                            .build();
            budgetGoalRepository.save(entity);
        }
        userCacheEvictor.evictUser(userId);
    }
}
