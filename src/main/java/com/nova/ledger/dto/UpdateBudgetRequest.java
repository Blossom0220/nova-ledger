package com.nova.ledger.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateBudgetRequest {
    @NotNull
    private BigDecimal budgetAmount;
}
