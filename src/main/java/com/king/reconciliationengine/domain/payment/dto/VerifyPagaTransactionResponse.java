package com.king.reconciliationengine.domain.payment.dto;

public record VerifyPagaTransactionResponse(
        String status_code,
        String status_message,
        String chargeId,
        int amount,
        String currency
) {
}
