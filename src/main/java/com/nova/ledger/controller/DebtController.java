package com.nova.ledger.controller;

import com.nova.ledger.dto.ApiResponse;
import com.nova.ledger.entity.Debt;
import com.nova.ledger.service.DebtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ledger/books/{bookId}/debts")
@RequiredArgsConstructor
public class DebtController {

    private final DebtService debtService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Debt>>> getDebts(@PathVariable Long bookId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(debtService.getDebts(bookId, userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Debt>> createDebt(
            @PathVariable Long bookId, @Valid @RequestBody Debt debt, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        debt.setUserId(userId);
        debt.setBookId(bookId);
        return ResponseEntity.ok(ApiResponse.success("创建成功", debtService.createDebt(debt)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Debt>> updateDebt(
            @PathVariable Long bookId, @PathVariable Long id,
            @Valid @RequestBody Debt debt, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("更新成功",
                debtService.updateDebt(id, userId, debt)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDebt(
            @PathVariable Long bookId, @PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        debtService.deleteDebt(id, userId);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }
}
