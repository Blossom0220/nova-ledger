package com.nova.ledger.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易记录+分类名+账户名的VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionVO {
    private Long id;
    private Long userId;
    private Long bookId;
    private Long accountId;
    private String accountName;
    private Long toAccountId;
    private String toAccountName;
    private Long categoryId;
    private String categoryName;
    private String type;
    private BigDecimal amount;
    private LocalDateTime transactionTime;
    private String note;
    private String merchant;
    private String tagIds;
    private String imageUrls;
    private Long billId;
}
