package com.king.reconciliationengine.domain.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Checkout request payload for initiating a payment")
public record CheckoutDto(
        @Schema(description = "Customer email address", example = "customer@example.com")
        @Email
        String email,

        @Schema(description = "Payment amount", example = "5000.00")
        @NotNull
        @Positive
        BigDecimal amount,

        @Schema(description = "Payment currency ISO code", example = "NGN")
        String currency
) {
}
