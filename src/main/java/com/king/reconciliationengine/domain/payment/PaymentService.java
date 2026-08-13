package com.king.reconciliationengine.domain.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.king.reconciliationengine.domain.idempotencykey.IdempotencyKeyRepository;
import com.king.reconciliationengine.domain.idempotencykey.entity.IdempotencyKey;
import com.king.reconciliationengine.domain.idempotencykey.enums.IdempotencyKeyStatus;
import com.king.reconciliationengine.domain.payment.dto.CheckoutDto;
import com.king.reconciliationengine.domain.payment.entity.Payment;
import com.king.reconciliationengine.domain.payment.enums.PaymentStatus;
import com.king.reconciliationengine.domain.user.UserService;
import com.king.reconciliationengine.infrastructure.paymentgateway.paga.PagaClient;
import com.king.reconciliationengine.infrastructure.paymentgateway.paga.dto.PagaCheckoutRequest;
import com.king.reconciliationengine.infrastructure.paymentgateway.paga.dto.PagaCheckoutResponse;
import com.king.reconciliationengine.common.response.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class PaymentService {
    private final UserService userService;
    private final PagaClient pagaClient;
    private final PaymentRepository paymentRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    @Value("${payload.secret.key}")
    private final String payloadSecret;

    public PagaCheckoutResponse checkout(CheckoutDto payload, String userId, String idempotencyKey) {
        userService.getById(userId);

        Optional<IdempotencyKey> existing = idempotencyKeyRepository.findByValue(idempotencyKey);

        if (existing.isPresent()) {
            IdempotencyKey record = existing.get();


            return switch (record.getStatus()) {
                case PENDING -> throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Request already in progress"
                );
                case COMPLETED, FAILED_TERMINAL -> {
                    try {
                        yield objectMapper.readValue(record.getResponse(), PagaCheckoutResponse.class);
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to deserialize stored response", e);
                    }
                }
                case UNKNOWN -> throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Previous request outcome unknown. Requires manual verification"
                );
            };
        }

        String txRef = "pay_" + UUID.randomUUID().toString().substring(0, 12);

        Payment paymentInstance = Payment.builder()
                .amount(payload.amount())
                .currency("NGN")
                .paymentStatus(PaymentStatus.PENDING)
                .reference(txRef)
                .build();

        paymentRepository.save(paymentInstance);

        String payloadHash = hashPayload(payload, userId);

        IdempotencyKey idempotencyKeyRecord = IdempotencyKey.builder()
                .value(idempotencyKey)
                .status(IdempotencyKeyStatus.PENDING)
                .requestHash(payloadHash)
                .payment(paymentInstance)
                .response(null)
                .build();

        idempotencyKeyRepository.save(idempotencyKeyRecord);

        PagaCheckoutRequest pagaPayload = PagaCheckoutRequest.builder()
                .email(payload.email())
                .amount(payload.amount())
                .currency("NGN")
                .payment_reference(txRef)
                .build();

        PagaCheckoutResponse response;
        try {
            response = pagaClient.checkout(pagaPayload);
        } catch (Exception e) {
            idempotencyKeyRecord.setStatus(IdempotencyKeyStatus.UNKNOWN);
            idempotencyKeyRepository.save(idempotencyKeyRecord);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Payment gateway did not respond — status unknown, do not retry blindly", e);
        }

        try {
            idempotencyKeyRecord.setResponse(objectMapper.writeValueAsString(response));
            idempotencyKeyRepository.save(idempotencyKeyRecord);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to stringify gateway response", e);
        }

        return response;
    }

    private String hashPayload(CheckoutDto checkoutDto, String userId) {
        try {
            String payload = userId + "|" + checkoutDto.email() + "|" + checkoutDto.amount();
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(payloadSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create payload hash: " + e);
        }
    }
}