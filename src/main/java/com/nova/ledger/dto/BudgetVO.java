package com.nova.ledger.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class BudgetVO {
    private Long id;
    private Long userId;
    private Long bookId;
    private Long categoryId;
    private String categoryName;
    private String month;          // YYYY-MM
    private BigDecimal budgetAmount;
    private BigDecimal spentAmount;
    private boolean overBudget;
}
