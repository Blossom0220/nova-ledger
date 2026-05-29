package com.nova.ledger.controller;

import com.nova.ledger.dto.ApiResponse;
import com.nova.ledger.entity.Account;
import com.nova.ledger.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ledger/books/{bookId}/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Account>>> getAccounts(@PathVariable Long bookId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(accountService.getBookAccounts(bookId, userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Account>> createAccount(
            @PathVariable Long bookId,
            @Valid @RequestBody Account account,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        account.setUserId(userId);
        account.setBookId(bookId);
        return ResponseEntity.ok(ApiResponse.success("账户创建成功", accountService.createAccount(account)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Account>> updateAccount(
            @PathVariable Long bookId,
            @PathVariable Long id,
            @Valid @RequestBody Account account,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("账户更新成功",
                accountService.updateAccount(id, userId, account)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @PathVariable Long bookId,
            @PathVariable Long id,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        accountService.deleteAccount(id, userId);
        return ResponseEntity.ok(ApiResponse.success("账户删除成功", null));
    }

    @PostMapping("/{id}/recalculate")
    public ResponseEntity<ApiResponse<Void>> recalculate(
            @PathVariable Long bookId,
            @PathVariable Long id,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        accountService.recalculateBalance(id, userId);
        return ResponseEntity.ok(ApiResponse.success("余额已重新计算", null));
    }
}
