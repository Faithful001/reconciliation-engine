package com.king.reconciliationengine.domain.payment;

import com.king.reconciliationengine.common.response.Response;
import com.king.reconciliationengine.domain.idempotencykey.IdempotencyKeyRepository;
import com.king.reconciliationengine.domain.idempotencykey.entity.IdempotencyKey;
import com.king.reconciliationengine.domain.idempotencykey.enums.IdempotencyKeyStatus;
import com.king.reconciliationengine.domain.payment.dto.CheckoutDto;
import com.king.reconciliationengine.domain.payment.dto.GetPaymentStatusResponseData;
import com.king.reconciliationengine.domain.payment.entity.Payment;
import com.king.reconciliationengine.domain.payment.enums.PaymentStatus;
import com.king.reconciliationengine.domain.user.UserService;
import com.king.reconciliationengine.domain.user.entity.User;
import com.king.reconciliationengine.infrastructure.paymentgateway.paga.PagaClient;
import com.king.reconciliationengine.infrastructure.paymentgateway.paga.dto.PagaCheckoutRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final UserService userService;
    private final PagaClient pagaClient;
    private final PaymentRepository paymentRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Value("${payload.secret-key}")
    private final String payloadSecret;

    @Value("${paga.public-key}")
    private final String pagaPublicKey;

    @Value("${webhook-callback-url}")
    private final String webhookCallbackUrl;

    @Transactional
    public ResponseEntity<Response<String>> checkout(CheckoutDto payload, UUID userId, String idempotencyKey) {
        User user = userService.getById(userId);

        Optional<IdempotencyKey> existing = idempotencyKeyRepository.findByValue(idempotencyKey);
        String payloadHash = hashPayload(payload, userId);

        if (existing.isPresent()) {
            IdempotencyKey record = existing.get();

            if (!record.getRequestHash().equals(payloadHash)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Idempotency key reused with a different request payload");
            }

            return switch (record.getStatus()) {
                case PENDING -> {
                    if (record.getResponse() == null) {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT, "Request already in progress");
                    }
                    yield ResponseEntity.ok(Response.success("Checkout already initiated", record.getResponse()));
                }
                case RESOLVED -> {
                    Payment payment = record.getPayment();
                    boolean succeeded = payment.getStatus() == PaymentStatus.CAPTURED;
                    String message = succeeded ? "Payment already completed" : "Payment already failed";

                    if (!succeeded) {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT, message + " — retry with a new idempotency key");
                    }
                    yield ResponseEntity.ok(Response.success(message, payment.getReference()));
                }
                case UNKNOWN -> throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Previous request outcome unknown. Requires manual verification");
            };
        }

        String txRef = "pay_" + UUID.randomUUID().toString().substring(0, 12);
        String currency = payload.currency() != null ? payload.currency() : "NGN";

        Payment paymentInstance = Payment.builder()
                .amount(payload.amount())
                .currency(currency)
                .status(PaymentStatus.PENDING)
                .reference(txRef)
                .user(user)
                .build();

        paymentRepository.save(paymentInstance);

        PagaCheckoutRequest pagaPayload = PagaCheckoutRequest.builder()
                .publicKey(pagaPublicKey)
                .email(payload.email())
                .amount(payload.amount())
                .currency(currency)
                .paymentReference(txRef)
                .callbackUrl(webhookCallbackUrl)
                .build();

        String checkoutLink = pagaClient.buildCheckoutLink(pagaPayload);

        IdempotencyKey idempotencyKeyRecord = IdempotencyKey.builder()
                .value(idempotencyKey)
                .status(IdempotencyKeyStatus.PENDING)
                .requestHash(payloadHash)
                .payment(paymentInstance)
                .response(checkoutLink)
                .build();

        try {
            idempotencyKeyRepository.save(idempotencyKeyRecord);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request already in progress");
        }

        return ResponseEntity.ok(Response.success("Checkout successful", checkoutLink));
    }

    public ResponseEntity<Response<GetPaymentStatusResponseData>> getStatus(String reference, UUID userId) {
        Payment payment = paymentRepository.findByReferenceAndUserId(reference, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Payment not found"));

        GetPaymentStatusResponseData dto = new GetPaymentStatusResponseData(
                payment.getReference(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getCurrency()
        );

        return ResponseEntity.ok(Response.success("Payment status retrieved", dto));
    }

    private String hashPayload(CheckoutDto checkoutDto, UUID userId) {
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