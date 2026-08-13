package com.king.reconciliationengine.domain.payment.dto;

import com.king.reconciliationengine.domain.payment.enums.PaymentStatus;

import java.math.BigDecimal;

public record GetPaymentStatusResponseData(
        String reference,
        PaymentStatus status,
        BigDecimal amount,
        String currency
) {
}