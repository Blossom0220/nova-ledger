package com.nova.ledger.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateBudgetRequest {
    @NotNull
    private Long categoryId;
    @NotNull
    private String month;           // YYYY-MM
    @NotNull
    private BigDecimal budgetAmount;
}
