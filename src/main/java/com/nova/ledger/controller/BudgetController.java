package com.nova.ledger.controller;

import com.nova.ledger.dto.ApiResponse;
import com.nova.ledger.dto.BudgetVO;
import com.nova.ledger.dto.CreateBudgetRequest;
import com.nova.ledger.dto.UpdateBudgetRequest;
import com.nova.ledger.entity.Budget;
import com.nova.ledger.entity.Category;
import com.nova.ledger.entity.Transaction;
import com.nova.ledger.repository.BudgetRepository;
import com.nova.ledger.repository.CategoryRepository;
import com.nova.ledger.repository.TransactionRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ledger/books/{bookId}/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetRepository budgetRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<BudgetVO>>> getBudgets(
            @PathVariable Long bookId,
            @RequestParam(required = false) String month,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();

        YearMonth ym;
        if (month != null && !month.isBlank()) {
            ym = YearMonth.parse(month, DateTimeFormatter.ofPattern("yyyy-MM"));
        } else {
            ym = YearMonth.now();
        }

        List<Budget> budgets = budgetRepository.findByBookIdAndDeletedFalse(bookId);

        // 过滤 MONTHLY + startDate 在本月
        List<Budget> matched = budgets.stream()
                .filter(b -> b.getStartDate() != null
                        && b.getPeriod() == Budget.BudgetPeriod.MONTHLY
                        && YearMonth.from(b.getStartDate()).equals(ym))
                .collect(Collectors.toList());

        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();

        List<Object[]> sums = transactionRepository.sumGroupByCategory(
                bookId, Transaction.TransactionType.EXPENSE,
                monthStart.atStartOfDay(), monthEnd.plusDays(1).atStartOfDay());
        Map<Long, BigDecimal> spentMap = sums.stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (BigDecimal) row[1]));

        List<Category> allCats = categoryRepository.findByBookIdAndDeletedFalseOrderBySortOrderAsc(bookId);
        Map<Long, String> catNameMap = allCats.stream()
                .collect(Collectors.toMap(Category::getId, c -> c.getName() != null ? c.getName() : ""));

        List<BudgetVO> result = new ArrayList<>();
        for (Budget b : matched) {
            BigDecimal spent = spentMap.getOrDefault(b.getCategoryId(), BigDecimal.ZERO);
            boolean overBudget = spent.compareTo(b.getAmount()) >= 0;
            String catName = catNameMap.getOrDefault(b.getCategoryId(), "未分类");
            result.add(new BudgetVO(
                    b.getId(), userId, bookId,
                    b.getCategoryId(), catName,
                    ym.format(DateTimeFormatter.ofPattern("yyyy-MM")),
                    b.getAmount(), spent, overBudget));
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BudgetVO>> createBudget(
            @PathVariable Long bookId,
            @Valid @RequestBody CreateBudgetRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        YearMonth ym = YearMonth.parse(request.getMonth(), DateTimeFormatter.ofPattern("yyyy-MM"));

        Budget budget = Budget.builder()
                .userId(userId)
                .bookId(bookId)
                .categoryId(request.getCategoryId())
                .amount(request.getBudgetAmount())
                .period(Budget.BudgetPeriod.MONTHLY)
                .startDate(ym.atDay(1))
                .endDate(ym.atEndOfMonth())
                .notifyThreshold(new BigDecimal("80"))
                .build();

        Budget saved = budgetRepository.save(budget);

        String catName = categoryRepository.findById(request.getCategoryId())
                .map(Category::getName).orElse("未分类");

        BudgetVO vo = new BudgetVO(
                saved.getId(), userId, bookId,
                saved.getCategoryId(), catName,
                request.getMonth(), saved.getAmount(),
                BigDecimal.ZERO, false);

        return ResponseEntity.ok(ApiResponse.success("预算创建成功", vo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BudgetVO>> updateBudget(
            @PathVariable Long bookId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateBudgetRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();

        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("预算不存在"));
        if (!budget.getUserId().equals(userId) || budget.getDeleted()) {
            throw new RuntimeException("无权访问该预算");
        }

        budget.setAmount(request.getBudgetAmount());
        budgetRepository.save(budget);

        LocalDate s = budget.getStartDate();
        LocalDate e = budget.getEndDate() != null ? budget.getEndDate() : s.plusMonths(1).minusDays(1);
        BigDecimal spent = transactionRepository.sumExpenseByCategoryAndTimeRange(
                bookId, budget.getCategoryId(), s.atStartOfDay(), e.plusDays(1).atStartOfDay());
        if (spent == null) spent = BigDecimal.ZERO;

        String catName = categoryRepository.findById(budget.getCategoryId())
                .map(Category::getName).orElse("未分类");

        BudgetVO vo = new BudgetVO(
                id, userId, bookId,
                budget.getCategoryId(), catName,
                YearMonth.from(s).format(DateTimeFormatter.ofPattern("yyyy-MM")),
                budget.getAmount(), spent,
                spent.compareTo(budget.getAmount()) >= 0);

        return ResponseEntity.ok(ApiResponse.success("预算更新成功", vo));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(
            @PathVariable Long bookId,
            @PathVariable Long id,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("预算不存在"));
        if (!budget.getUserId().equals(userId)) {
            throw new RuntimeException("无权访问该预算");
        }
        budget.setDeleted(true);
        budgetRepository.save(budget);
        return ResponseEntity.ok(ApiResponse.success("预算删除成功", null));
    }
}
