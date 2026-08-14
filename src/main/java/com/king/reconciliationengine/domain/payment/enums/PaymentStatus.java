package com.king.reconciliationengine.domain.payment.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Lifecycle status of a payment transaction")
public enum PaymentStatus {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    TIMED_OUT,
    REFUNDED
}
