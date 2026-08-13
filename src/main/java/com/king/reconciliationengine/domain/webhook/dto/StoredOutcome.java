package com.king.reconciliationengine.domain.webhook.dto;

import com.king.reconciliationengine.infrastructure.paymentgateway.paga.dto.PagaVerifyResponse;

public record StoredOutcome(
        boolean success,
        String statusCode,
        String message,
        Data data
) {
    public static StoredOutcome from(PagaVerifyResponse response) {
        return new StoredOutcome(
                response.isSuccess(),
                String.valueOf(response.status_code()),
                response.status_message(),
                new Data(response.chargeId())
        );
    }

    public static StoredOutcome from(WebhookPayload payload) {
        boolean succeeded = "0".equals(payload.statusCode());
        return new StoredOutcome(
                succeeded,
                payload.statusCode(),
                payload.statusMessage(),
                new Data(payload.paymentReference())
        );
    }

    public record Data(
            String paymentReference
    ) {
    }
}