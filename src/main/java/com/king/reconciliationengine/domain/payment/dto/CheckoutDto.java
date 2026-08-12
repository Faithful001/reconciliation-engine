package com.king.reconciliationengine.domain.payment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record CheckoutDto(
        @Email
        String email,

        @NotNull
        @Positive
        BigDecimal amount
) {
}
