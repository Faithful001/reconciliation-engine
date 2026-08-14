package com.king.reconciliationengine.domain.webhook.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Payment gateway webhook notification payload")
public record WebhookPayload(
        @Schema(description = "Response status code ('0' indicating success)", example = "0")
        @NotBlank
        String statusCode,

        @Schema(description = "Response status message", example = "success")
        @NotBlank
        String statusMessage,

        @Schema(description = "Payment reference string", example = "Paga_Auto_Ref_20250826_132929_yjmb")
        @NotBlank
        String paymentReference,

        @Schema(description = "Transaction amount string", example = "10.00")
        @NotBlank
        String amount,

        @Schema(description = "ISO currency code", example = "NGN")
        @NotBlank
        String currency,

        @Schema(description = "ISO timestamp string", example = "2025-08-26T13:36:04")
        @NotBlank
        String timeStamp,

        @Schema(description = "Optional payment description", example = "Payment for order #1234")
        String description,

        @Schema(description = "Customer email address", example = "testemail2@gmail.com")
        @NotBlank
        String customerEmail,

        @Schema(description = "Customer phone number", example = "+2348063334156")
        @NotBlank
        String customerPhoneNumber,

        @Schema(description = "HMAC SHA-512 payload signature hash", example = "39453c520890841fe3a8377...")
        @NotBlank
        String hash
) {
}
