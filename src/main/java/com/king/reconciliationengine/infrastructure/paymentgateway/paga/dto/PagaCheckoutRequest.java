package com.king.reconciliationengine.infrastructure.paymentgateway.paga.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PagaCheckoutRequest(
        @NotBlank
        String publicKey, // internal

        @NotNull
        @Positive
        BigDecimal amount,

        String currency,	// internal
        String paymentReference, // internal
        String chargeUrl,	// internal
        String phoneNumber, //	internal

        @Email
        String email,
        String displayImage,	// internal
        String callbackUrl,	// internal
        String fundingSources	// internal - No

) {
}
