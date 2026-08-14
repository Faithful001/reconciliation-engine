package com.king.reconciliationengine.domain.payment;

import com.king.reconciliationengine.common.response.Response;
import com.king.reconciliationengine.domain.payment.dto.CheckoutDto;
import com.king.reconciliationengine.domain.payment.dto.GetPaymentStatusResponseData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
@Tag(name = "Payment", description = "Payment processing, checkout, and status lookups")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @Operation(summary = "Initiate payment checkout", description = "Initiates a new payment transaction with idempotency key protection.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment checkout initiated successfully"),
            @ApiResponse(responseCode = "400", description = "Missing or invalid payload / Idempotency-Key header"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token required"),
            @ApiResponse(responseCode = "409", description = "Idempotency conflict - Concurrent request with same key")
    })
    @PostMapping("/checkout")
    public ResponseEntity<Response<String>> checkout(
            @Valid @RequestBody CheckoutDto payload,
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId,
            @Parameter(name = "Idempotency-Key", description = "Unique key to ensure request idempotency", required = true, example = "idemp_8f7b2a9c-1234")
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        return paymentService.checkout(payload, userId, idempotencyKey);
    }

    @Operation(summary = "Get payment status", description = "Retrieves the current status of a payment transaction by reference.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment status fetched successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Valid JWT token required"),
            @ApiResponse(responseCode = "404", description = "Payment reference not found")
    })
    @GetMapping("/{reference}/status")
    public ResponseEntity<Response<GetPaymentStatusResponseData>> getStatus(
            @Parameter(description = "Payment transaction reference", example = "Paga_Auto_Ref_20250826_132929_yjmb")
            @PathVariable String reference,
            @Parameter(hidden = true) @AuthenticationPrincipal UUID userId
    ) {
        return paymentService.getStatus(reference, userId);
    }
}