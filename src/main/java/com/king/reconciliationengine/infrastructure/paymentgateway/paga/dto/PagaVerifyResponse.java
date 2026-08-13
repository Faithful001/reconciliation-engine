package com.king.reconciliationengine.infrastructure.paymentgateway.paga.dto;

public record PagaVerifyResponse(
        Integer status_code,
        String status_message,
        String chargeId,
        Double amount,
        String currency
) {
    public boolean isSuccess() {
        return status_code != null && status_code == 0;
    }

    public boolean isPending() {
        return status_code != null && status_code == 2;
    }
}