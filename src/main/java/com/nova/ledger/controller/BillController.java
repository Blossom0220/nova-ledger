package com.nova.ledger.controller;

import com.nova.ledger.dto.ApiResponse;
import com.nova.ledger.entity.Bill;
import com.nova.ledger.service.BillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ledger/books/{bookId}/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Bill>>> getBills(@PathVariable Long bookId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(billService.getBills(bookId, userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Bill>> createBill(
            @PathVariable Long bookId, @Valid @RequestBody Bill bill, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        bill.setUserId(userId);
        bill.setBookId(bookId);
        return ResponseEntity.ok(ApiResponse.success("周期账单创建成功", billService.createBill(bill)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Bill>> updateBill(
            @PathVariable Long bookId, @PathVariable Long id,
            @Valid @RequestBody Bill bill, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success("周期账单更新成功",
                billService.updateBill(id, userId, bill)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBill(
            @PathVariable Long bookId, @PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        billService.deleteBill(id, userId);
        return ResponseEntity.ok(ApiResponse.success("周期账单删除成功", null));
    }
}
