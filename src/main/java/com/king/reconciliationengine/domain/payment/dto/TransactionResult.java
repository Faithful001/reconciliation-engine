package com.king.reconciliationengine.domain.payment.dto;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
public class TransactionResult {
    private String reference;
    private BigDecimal amount;
    private String currency;
}
