package com.nova.ledger.repository;

import com.nova.ledger.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByBookIdAndDeletedFalseOrderByTransactionTimeDesc(Long bookId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.bookId = :bookId AND t.deleted = false " +
           "AND (:startDate IS NULL OR t.transactionTime >= :startDate) " +
           "AND (:endDate IS NULL OR t.transactionTime <= :endDate) " +
           "AND (:categoryId IS NULL OR t.categoryId = :categoryId) " +
           "AND (:accountId IS NULL OR t.accountId = :accountId) " +
           "AND (:type IS NULL OR t.type = :type) " +
           "AND (:keyword IS NULL OR t.note LIKE %:keyword% OR t.merchant LIKE %:keyword%) " +
           "ORDER BY t.transactionTime DESC")
    Page<Transaction> searchTransactions(
            @Param("bookId") Long bookId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("categoryId") Long categoryId,
            @Param("accountId") Long accountId,
            @Param("type") Transaction.TransactionType type,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.bookId = :bookId AND t.deleted = false " +
           "AND t.type = :type " +
           "AND t.transactionTime BETWEEN :start AND :end")
    BigDecimal sumByTypeAndTimeRange(
            @Param("bookId") Long bookId,
            @Param("type") Transaction.TransactionType type,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.bookId = :bookId AND t.deleted = false " +
           "AND t.type = 'EXPENSE' " +
           "AND t.categoryId = :categoryId " +
           "AND t.transactionTime BETWEEN :start AND :end")
    BigDecimal sumExpenseByCategoryAndTimeRange(
            @Param("bookId") Long bookId,
            @Param("categoryId") Long categoryId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT t.categoryId, COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.bookId = :bookId AND t.deleted = false " +
           "AND t.type = :type " +
           "AND t.transactionTime BETWEEN :start AND :end " +
           "GROUP BY t.categoryId")
    List<Object[]> sumGroupByCategory(
            @Param("bookId") Long bookId,
            @Param("type") Transaction.TransactionType type,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT FUNCTION('DATE_FORMAT', t.transactionTime, :dateFormat), COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.bookId = :bookId AND t.deleted = false " +
           "AND t.type = :type " +
           "AND t.transactionTime BETWEEN :start AND :end " +
           "GROUP BY FUNCTION('DATE_FORMAT', t.transactionTime, :dateFormat) " +
           "ORDER BY FUNCTION('DATE_FORMAT', t.transactionTime, :dateFormat)")
    List<Object[]> sumByTimeSeries(
            @Param("bookId") Long bookId,
            @Param("type") Transaction.TransactionType type,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("dateFormat") String dateFormat);

    List<Transaction> findByBookIdAndDeletedFalseAndTransactionTimeBetween(
            Long bookId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.accountId = :accountId AND t.deleted = false " +
           "AND t.type = 'INCOME'")
    BigDecimal sumIncomeByAccount(@Param("accountId") Long accountId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.accountId = :accountId AND t.deleted = false " +
           "AND t.type = 'EXPENSE'")
    BigDecimal sumExpenseByAccount(@Param("accountId") Long accountId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.accountId = :accountId AND t.deleted = false " +
           "AND t.type = 'EXPENSE' AND t.toAccountId = :toAccountId")
    BigDecimal sumTransferOut(@Param("accountId") Long accountId, @Param("toAccountId") Long toAccountId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.toAccountId = :accountId AND t.deleted = false " +
           "AND t.type = 'TRANSFER'")
    BigDecimal sumTransferIn(@Param("accountId") Long accountId);
}
