package com.king.reconciliationengine.domain.webhook.dto;

import jakarta.validation.constraints.NotBlank;

public record WebhookPayload(
        @NotBlank
        String statusCode, // "0" means successful,

        @NotBlank
        String statusMessage, // example - "success",

        @NotBlank
        String paymentReference, // e.g: "Paga_Auto_Ref_20250826_132929_yjmb",

        @NotBlank
        String amount, // e.g: "10.00"

        @NotBlank
        String  currency, // e.g: "NGN",

        @NotBlank
        String timeStamp, // e.g: "2025-08-26T13:36:04",

        String description, //e.g: null,

        @NotBlank
        String customerEmail, // e.g: "testemail2@gmail.com",

        @NotBlank
        String customerPhoneNumber, // e.g: "+2348063334156",

        @NotBlank
        String hash // "39453c520890841fe3a837701e60b7dba3f8d737696d8fa5a39e8566b9f96948eaac792e7c03783e67033c37bf6d123e1ff181f015e729e1bcfa70bd8a16ae97"

) {
}
