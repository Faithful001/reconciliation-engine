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
        String public_key, // internal

        @NotNull
        @Positive
        BigDecimal amount,

        String currency,	// internal
        String payment_reference, // internal
        String charge_url,	// internal
        String phone_number, //	internal

        @Email
        String email,
        String display_image,	// internal
        String callback_url,	// internal
        String funding_sources	// internal - No

) {
}
