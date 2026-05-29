package com.nova.ledger.controller;

import com.nova.ledger.dto.ApiResponse;
import com.nova.ledger.entity.Transaction;
import com.nova.ledger.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/ledger/books/{bookId}/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<StatsService.OverviewVO>> overview(
            @PathVariable Long bookId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(statsService.getOverview(bookId, userId)));
    }

    @GetMapping({"/category", "/by-category"})
    public ResponseEntity<ApiResponse<List<StatsService.CategoryStatsVO>>> categoryStats(
            @PathVariable Long bookId,
            @RequestParam(required = false) Transaction.TransactionType type,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        if (start == null) {
            start = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        }
        if (end == null) {
            end = LocalDate.now().plusMonths(1).withDayOfMonth(1).atStartOfDay();
        }
        if (type == null) {
            type = Transaction.TransactionType.EXPENSE;
        }
        return ResponseEntity.ok(ApiResponse.success(
                statsService.getCategoryStats(bookId, userId, type, start, end)));
    }

    @GetMapping("/trend")
    public ResponseEntity<ApiResponse<List<StatsService.TimeSeriesVO>>> trend(
            @PathVariable Long bookId,
            @RequestParam Transaction.TransactionType type,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "%Y-%m-%d") String dateFormat,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(
                statsService.getTrend(bookId, userId, type, start, end, dateFormat)));
    }

    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<List<StatsService.DailyStatsVO>>> daily(
            @PathVariable Long bookId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(
                statsService.getDailyStats(bookId, userId, startDate, endDate)));
    }

    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<List<StatsService.AccountBalanceVO>>> accountBalances(
            @PathVariable Long bookId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(ApiResponse.success(statsService.getAccountBalances(bookId, userId)));
    }
}
