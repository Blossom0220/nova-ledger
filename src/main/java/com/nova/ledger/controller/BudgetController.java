package com.nova.ledger.controller;

import com.nova.ledger.dto.ApiResponse;
import com.nova.ledger.entity.Budget;
import com.nova.ledger.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ledger/books/{bookId}/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Budget>>> getBudgets(@PathVariable Long bookId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(budgetService.getBudgets(bookId, userId)));
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<List<BudgetService.BudgetOverview>>> getOverview(
            @PathVariable Long bookId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(budgetService.getBudgetOverview(bookId, userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Budget>> createBudget(
            @PathVariable Long bookId, @Valid @RequestBody Budget budget, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        budget.setUserId(userId);
        budget.setBookId(bookId);
        return ResponseEntity.ok(ApiResponse.success("预算创建成功", budgetService.createBudget(budget)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Budget>> updateBudget(
            @PathVariable Long bookId, @PathVariable Long id,
            @Valid @RequestBody Budget budget, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("预算更新成功",
                budgetService.updateBudget(id, userId, budget)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(
            @PathVariable Long bookId, @PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        budgetService.deleteBudget(id, userId);
        return ResponseEntity.ok(ApiResponse.success("预算删除成功", null));
    }
}
