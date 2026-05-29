package com.nova.ledger.service;

import com.nova.ledger.entity.Category;
import com.nova.ledger.entity.Transaction;
import com.nova.ledger.repository.AccountRepository;
import com.nova.ledger.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final TransactionService transactionService;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;

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

    public List<CategoryStatsVO> getCategoryStats(Long bookId, Long userId,
                                                   Transaction.TransactionType type,
                                                   LocalDateTime start, LocalDateTime end) {
        List<Object[]> raw = transactionService.sumByCategory(bookId, type, start, end);
        Map<Long, Category> categoryMap = categoryRepository.findByBookIdAndDeletedFalseOrderBySortOrderAsc(bookId)
                .stream().collect(Collectors.toMap(Category::getId, c -> c));

        BigDecimal total = raw.stream()
                .map(row -> (BigDecimal) row[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategoryStatsVO> result = new ArrayList<>();
        for (Object[] row : raw) {
            Long categoryId = (Long) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            Category cat = categoryMap.get(categoryId);
            String name = cat != null ? cat.getName() : "\u672a\u5206\u7c7b";
            String color = cat != null && cat.getColor() != null ? cat.getColor() : "#909399";
            double pct = total.compareTo(BigDecimal.ZERO) > 0
                    ? amount.multiply(new BigDecimal("100")).divide(total, 1, RoundingMode.HALF_UP).doubleValue()
                    : 0;
            result.add(new CategoryStatsVO(categoryId, name, amount, color, pct));
        }
        return result;
    }

    public List<TimeSeriesVO> getTrend(Long bookId, Long userId,
                                        Transaction.TransactionType type,
                                        LocalDateTime start, LocalDateTime end, String dateFormat) {
        List<Object[]> raw = transactionService.trendByTimeSeries(bookId, type, start, end, dateFormat);
        return raw.stream()
                .map(row -> new TimeSeriesVO((String) row[0], (BigDecimal) row[1]))
                .collect(Collectors.toList());
    }

    public List<DailyStatsVO> getDailyStats(Long bookId, Long userId,
                                             LocalDateTime start, LocalDateTime end) {
        List<Object[]> incomeRaw = transactionService.trendByTimeSeries(bookId, Transaction.TransactionType.INCOME, start, end, "%Y-%m-%d");
        List<Object[]> expenseRaw = transactionService.trendByTimeSeries(bookId, Transaction.TransactionType.EXPENSE, start, end, "%Y-%m-%d");

        Map<String, BigDecimal> incomeMap = new HashMap<>();
        Map<String, BigDecimal> expenseMap = new HashMap<>();
        for (Object[] row : incomeRaw) incomeMap.put((String) row[0], (BigDecimal) row[1]);
        for (Object[] row : expenseRaw) expenseMap.put((String) row[0], (BigDecimal) row[1]);

        Set<String> allDates = new LinkedHashSet<>();
        allDates.addAll(incomeMap.keySet());
        allDates.addAll(expenseMap.keySet());

        List<DailyStatsVO> result = new ArrayList<>();
        for (String date : allDates) {
            result.add(new DailyStatsVO(date,
                    incomeMap.getOrDefault(date, BigDecimal.ZERO),
                    expenseMap.getOrDefault(date, BigDecimal.ZERO)));
        }
        result.sort(Comparator.comparing(DailyStatsVO::date));
        return result;
    }

    public List<AccountBalanceVO> getAccountBalances(Long bookId, Long userId) {
        return accountRepository.findByBookIdAndDeletedFalseOrderBySortOrderAsc(bookId)
                .stream()
                .map(a -> new AccountBalanceVO(a.getId(), a.getName(), a.getType().name(), a.getBalance(), a.getCurrency()))
                .collect(Collectors.toList());
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

    public record CategoryStatsVO(Long categoryId, String categoryName, BigDecimal amount,
                                   String color, double percentage) {}

    public record TimeSeriesVO(String date, BigDecimal amount) {}

    public record DailyStatsVO(String date, BigDecimal income, BigDecimal expense) {}

    public record AccountBalanceVO(Long accountId, String name, String type, BigDecimal balance, String currency) {}
}
