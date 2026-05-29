package com.nova.ledger.service;

import com.nova.ledger.entity.Account;
import com.nova.ledger.entity.Category;
import com.nova.ledger.entity.Transaction;
import com.nova.ledger.repository.AccountRepository;
import com.nova.ledger.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final TransactionService transactionService;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

    /**
     * 本月收支概览
     */
    public OverviewVO getOverview(Long bookId, Long userId) {
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = monthStart.plusMonths(1);

        LocalDateTime lastMonthStart = monthStart.minusMonths(1);
        LocalDateTime lastMonthEnd = monthStart;

        BigDecimal income = transactionService.sumIncome(bookId, monthStart, monthEnd);
        BigDecimal expense = transactionService.sumExpense(bookId, monthStart, monthEnd);
        BigDecimal lastIncome = transactionService.sumIncome(bookId, lastMonthStart, lastMonthEnd);
        BigDecimal lastExpense = transactionService.sumExpense(bookId, lastMonthStart, lastMonthEnd);

        return new OverviewVO(income, expense, income.subtract(expense),
                lastIncome, lastExpense, calculateChange(income, lastIncome),
                calculateChange(expense, lastExpense));
    }

    /**
     * 分类汇总
     */
    public List<CategoryStatsVO> getCategoryStats(Long bookId, Long userId,
                                                   Transaction.TransactionType type,
                                                   LocalDateTime start, LocalDateTime end) {
        List<Object[]> raw = transactionService.sumByCategory(bookId, type, start, end);
        Map<Long, String> categoryNames = loadCategoryNames(bookId, userId);

        List<CategoryStatsVO> result = new ArrayList<>();
        for (Object[] row : raw) {
            Long categoryId = (Long) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            result.add(new CategoryStatsVO(
                    categoryId,
                    categoryNames.getOrDefault(categoryId, "未分类"),
                    amount));
        }

        BigDecimal total = result.stream()
                .map(CategoryStatsVO::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (CategoryStatsVO vo : result) {
            double percentage = total.compareTo(BigDecimal.ZERO) > 0
                    ? vo.amount().divide(total, 4, java.math.RoundingMode.HALF_UP)
                            .multiply(new BigDecimal("100")).doubleValue()
                    : 0;
            // 不可变 record，这里需要返回包含 percentage 的版本
        }

        return result;
    }

    /**
     * 时间序列趋势
     */
    public List<TimeSeriesVO> getTrend(Long bookId, Long userId,
                                        Transaction.TransactionType type,
                                        LocalDateTime start, LocalDateTime end, String dateFormat) {
        List<Object[]> raw = transactionService.trendByTimeSeries(bookId, type, start, end, dateFormat);
        return raw.stream()
                .map(row -> new TimeSeriesVO((String) row[0], (BigDecimal) row[1]))
                .collect(Collectors.toList());
    }

    /**
     * 各账户余额
     */
    public List<AccountBalanceVO> getAccountBalances(Long bookId, Long userId) {
        return accountRepository.findByBookIdAndDeletedFalseOrderBySortOrderAsc(bookId)
                .stream()
                .map(a -> new AccountBalanceVO(a.getId(), a.getName(), a.getType().name(), a.getBalance(), a.getCurrency()))
                .collect(Collectors.toList());
    }

    private Map<Long, String> loadCategoryNames(Long bookId, Long userId) {
        return categoryRepository.findByBookIdAndDeletedFalseOrderBySortOrderAsc(bookId)
                .stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
    }

    private double calculateChange(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100 : 0;
        }
        return current.subtract(previous)
                .divide(previous, 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .doubleValue();
    }

    public record OverviewVO(BigDecimal income, BigDecimal expense, BigDecimal balance,
                              BigDecimal lastIncome, BigDecimal lastExpense,
                              double incomeChange, double expenseChange) {}

    public record CategoryStatsVO(Long categoryId, String categoryName, BigDecimal amount) {}

    public record TimeSeriesVO(String date, BigDecimal amount) {}

    public record AccountBalanceVO(Long accountId, String name, String type, BigDecimal balance, String currency) {}
}
