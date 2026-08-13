package com.king.reconciliationengine.domain.payment;

import com.king.reconciliationengine.common.response.Response;
import com.king.reconciliationengine.domain.payment.dto.CheckoutDto;
import com.king.reconciliationengine.domain.payment.dto.GetPaymentStatusResponseData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/checkout")
    public ResponseEntity<Response<String>> checkout(
            @Valid @RequestBody CheckoutDto payload,
            @AuthenticationPrincipal String userId,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        return paymentService.checkout(payload, userId, idempotencyKey);
    }

    @GetMapping("/{reference}/status")
    public ResponseEntity<Response<GetPaymentStatusResponseData>> getStatus(@PathVariable String reference) {
        return paymentService.getStatus(reference);
    }
}