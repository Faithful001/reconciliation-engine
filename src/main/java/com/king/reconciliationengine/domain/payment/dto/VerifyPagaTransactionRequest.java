package com.king.reconciliationengine.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyPagaTransactionRequest(
        @NotBlank
        String paymentReference,

        @NotBlank
        String publicKey,

        @NotBlank
        String amount,

        @NotBlank
        String currency
) {
}
