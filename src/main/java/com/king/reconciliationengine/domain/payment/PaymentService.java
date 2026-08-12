package com.king.reconciliationengine.domain.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.king.reconciliationengine.domain.idempotencykey.IdempotencyKeyRepository;
import com.king.reconciliationengine.domain.idempotencykey.entity.IdempotencyKey;
import com.king.reconciliationengine.domain.idempotencykey.enums.IdempotencyKeyStatus;
import com.king.reconciliationengine.domain.payment.dto.CheckoutDto;
import com.king.reconciliationengine.domain.payment.dto.TransactionResult;
import com.king.reconciliationengine.domain.payment.entity.Payment;
import com.king.reconciliationengine.domain.payment.enums.PaymentStatus;
import com.king.reconciliationengine.domain.user.UserService;
import com.king.reconciliationengine.domain.user.entity.User;
import com.king.reconciliationengine.infrastructure.paymentgateway.paga.PagaClient;
import com.king.reconciliationengine.infrastructure.paymentgateway.paga.dto.PagaCheckoutRequest;
import com.king.reconciliationengine.infrastructure.paymentgateway.paga.dto.PagaCheckoutResponse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@RequiredArgsConstructor
@Getter
@Setter
@Service
public class PaymentService {
    private final UserService userService;
    private final PagaClient pagaClient;
    private final PaymentRepository paymentRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    private final ObjectMapper objectMapper;
    @Value("${payload.secret.key}")
    private final String payloadSecret;

    public PagaCheckoutResponse checkout(CheckoutDto payload, String userId, String idempotencyKey){
        // check if user exists
        userService.getById(userId);

        //generate transaction reference
        String txRef = "pay_" + UUID.randomUUID().toString().substring(0, 12) + userId + LocalDateTime.now();

        // create the payment record
        Payment paymentInstance = Payment.builder() // build the instance
                .amount(payload.amount())
                .currency("NGN")
                .paymentStatus(PaymentStatus.PENDING)
                .reference(txRef)
                .build();

        paymentRepository.save(paymentInstance); // create the record

        String payloadHash = hashPayload(payload, userId);

        TransactionResult transactionResult = new TransactionResult(txRef, payload.amount(), "NGN");
        String responseString;

        try {
            responseString = objectMapper.writeValueAsString(transactionResult);
        } catch(Exception e) {
            throw new IllegalStateException("Failed to stringify result");
        }

        // create the idempotency_key record
        IdempotencyKey idempotencyKeyRecord = IdempotencyKey.builder() // build the instance
                .value(idempotencyKey)
                .status(IdempotencyKeyStatus.PENDING)
                .requestHash(payloadHash)
                .response(responseString)
                .build();

        idempotencyKeyRepository.save(idempotencyKeyRecord); // create the record

        PagaCheckoutRequest pagaPayload = PagaCheckoutRequest.builder() // build the instance
                .email(payload.email())
                .amount(payload.amount())
                .currency("NGN")
                .payment_reference(txRef)
                .build();

        // initiate the checkout and return the response
        return pagaClient.checkout(pagaPayload);
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
