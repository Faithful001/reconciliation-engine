package com.king.reconciliationengine.infrastructure.paymentgateway.paga.dto;

import java.math.BigDecimal;

public record PagaVerifyRequest(
        String paymentReference,
        String publicKey,
        BigDecimal amount,
        String currency
) {
}