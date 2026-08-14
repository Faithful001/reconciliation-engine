package com.king.reconciliationengine.domain.payment.dto;

import com.king.reconciliationengine.domain.payment.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Payment transaction status response payload")
public record GetPaymentStatusResponseData(
        @Schema(description = "Unique payment reference identifier", example = "Paga_Auto_Ref_20250826_132929_yjmb")
        String reference,

        @Schema(description = "Current status of the payment transaction")
        PaymentStatus status,

        @Schema(description = "Payment amount", example = "5000.00")
        BigDecimal amount,

        @Schema(description = "Payment currency ISO code", example = "NGN")
        String currency
) {
}