package com.king.reconciliationengine.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyPagaTransactionDto(
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
