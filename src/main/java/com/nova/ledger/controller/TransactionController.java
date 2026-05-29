package com.nova.ledger.controller;

import com.nova.ledger.dto.ApiResponse;
import com.nova.ledger.dto.TransactionVO;
import com.nova.ledger.entity.Transaction;
import com.nova.ledger.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/ledger/books/{bookId}/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TransactionVO>>> searchTransactions(
            @PathVariable Long bookId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) Transaction.TransactionType type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Page<TransactionVO> result = transactionService.searchTransactions(
                bookId, userId, startDate, endDate, categoryId, accountId, type, keyword,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionTime")));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionVO>> getTransaction(
            @PathVariable Long bookId,
            @PathVariable Long id,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(transactionService.getTransactionVO(id, userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionVO>> createTransaction(
            @PathVariable Long bookId,
            @Valid @RequestBody Transaction transaction,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        transaction.setUserId(userId);
        transaction.setBookId(bookId);
        Transaction saved = transactionService.createTransaction(transaction);
        return ResponseEntity.ok(ApiResponse.success("记账成功",
                transactionService.getTransactionVO(saved.getId(), userId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionVO>> updateTransaction(
            @PathVariable Long bookId,
            @PathVariable Long id,
            @Valid @RequestBody Transaction transaction,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        transactionService.updateTransaction(id, userId, transaction);
        return ResponseEntity.ok(ApiResponse.success("更新成功",
                transactionService.getTransactionVO(id, userId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(
            @PathVariable Long bookId,
            @PathVariable Long id,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        transactionService.deleteTransaction(id, userId);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }
}
