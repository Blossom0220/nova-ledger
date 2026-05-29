package com.nova.ledger.service;

import com.nova.ledger.entity.Budget;
import com.nova.ledger.entity.Transaction;
import com.nova.ledger.repository.BudgetRepository;
import com.nova.ledger.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;

    public List<Budget> getBudgets(Long bookId, Long userId) {
        return budgetRepository.findByBookIdAndDeletedFalse(bookId);
    }

    @Transactional
    public Budget createBudget(Budget budget) {
        return budgetRepository.save(budget);
    }

    @Transactional
    public Budget updateBudget(Long id, Long userId, Budget update) {
        Budget budget = getBudget(id, userId);
        budget.setAmount(update.getAmount());
        budget.setPeriod(update.getPeriod());
        budget.setStartDate(update.getStartDate());
        budget.setEndDate(update.getEndDate());
        budget.setNotifyThreshold(update.getNotifyThreshold());
        return budgetRepository.save(budget);
    }

    @Transactional
    public void deleteBudget(Long id, Long userId) {
        Budget budget = getBudget(id, userId);
        budget.setDeleted(true);
        budgetRepository.save(budget);
    }

    public Budget getBudget(Long id, Long userId) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("预算不存在"));
        if (!budget.getUserId().equals(userId) || budget.getDeleted()) {
            throw new RuntimeException("无权访问该预算");
        }
        return budget;
    }

    /**
     * 获取预算概览：每个预算的已花费、剩余、是否预警
     */
    public List<BudgetOverview> getBudgetOverview(Long bookId, Long userId) {
        List<Budget> budgets = budgetRepository.findByBookIdAndDeletedFalse(bookId);
        List<BudgetOverview> overviews = new ArrayList<>();

        for (Budget budget : budgets) {
            LocalDateTime[] range = getBudgetTimeRange(budget);
            BigDecimal spent = transactionRepository.sumExpenseByCategoryAndTimeRange(
                    bookId, budget.getCategoryId(), range[0], range[1]);

            if (spent == null) spent = BigDecimal.ZERO;
            BigDecimal remaining = budget.getAmount().subtract(spent);
            BigDecimal percentage = budget.getAmount().compareTo(BigDecimal.ZERO) > 0
                    ? spent.divide(budget.getAmount(), 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;
            boolean alerted = percentage.compareTo(budget.getNotifyThreshold()) >= 0;

            overviews.add(new BudgetOverview(
                    budget.getId(), budget.getCategoryId(), budget.getAmount(),
                    spent, remaining, percentage, alerted, range[0], range[1]));
        }

        return overviews;
    }

    private LocalDateTime[] getBudgetTimeRange(Budget budget) {
        LocalDate now = LocalDate.now();
        return switch (budget.getPeriod()) {
            case WEEKLY -> {
                LocalDate weekStart = now.with(java.time.DayOfWeek.MONDAY);
                yield new LocalDateTime[]{
                        weekStart.atStartOfDay(),
                        weekStart.plusDays(7).atStartOfDay()
                };
            }
            case MONTHLY -> {
                LocalDate monthStart = now.withDayOfMonth(1);
                yield new LocalDateTime[]{
                        monthStart.atStartOfDay(),
                        monthStart.plusMonths(1).atStartOfDay()
                };
            }
            case YEARLY -> {
                LocalDate yearStart = now.withDayOfYear(1);
                yield new LocalDateTime[]{
                        yearStart.atStartOfDay(),
                        yearStart.plusYears(1).atStartOfDay()
                };
            }
            case CUSTOM -> new LocalDateTime[]{
                    budget.getStartDate().atStartOfDay(),
                    budget.getEndDate() != null
                            ? budget.getEndDate().plusDays(1).atStartOfDay()
                            : LocalDateTime.of(2099, 12, 31, 23, 59)
            };
        };
    }

    public record BudgetOverview(Long budgetId, Long categoryId, BigDecimal budgetAmount,
                                  BigDecimal spent, BigDecimal remaining, BigDecimal percentage,
                                  boolean alerted, LocalDateTime periodStart, LocalDateTime periodEnd) {}
}
