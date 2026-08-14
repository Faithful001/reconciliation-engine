package com.king.reconciliationengine.domain.payment.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The origin of a payment status transition")
public enum ChangeSource {
    CHECKOUT,
    WEBHOOK,
    RECONCILIATION
}
