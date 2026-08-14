package com.king.reconciliationengine.domain.webhook;

import com.king.reconciliationengine.common.response.Response;
import com.king.reconciliationengine.domain.idempotencykey.IdempotencyKeyRepository;
import com.king.reconciliationengine.domain.idempotencykey.entity.IdempotencyKey;
import com.king.reconciliationengine.domain.idempotencykey.enums.IdempotencyKeyStatus;
import com.king.reconciliationengine.domain.payment.PaymentRepository;
import com.king.reconciliationengine.domain.payment.PaymentStatusHistoryService;
import com.king.reconciliationengine.domain.webhook.dto.StoredOutcome;
import com.king.reconciliationengine.domain.payment.entity.Payment;
import com.king.reconciliationengine.domain.payment.enums.ChangeSource;
import com.king.reconciliationengine.domain.payment.enums.PaymentStatus;
import com.king.reconciliationengine.domain.webhook.dto.WebhookPayload;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private final PaymentRepository paymentRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final PaymentStatusHistoryService paymentStatusHistoryService;
    private final ObjectMapper objectMapper;

    @Value("${paga.webhook-secret}")
    private String webhookSecret;

    public ResponseEntity<Response<Void>> processCall(WebhookPayload payload) {
        boolean isValidSignature = verifyHash(
                payload.hash(),
                payload.amount(),
                payload.timeStamp(),
                payload.paymentReference()
        );

        if (!isValidSignature) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid webhook signature");
        }

        Payment payment = paymentRepository.findByReference(payload.paymentReference())
                .orElse(null);

        if (payment == null) {
            log.warn("Webhook received for unknown payment reference: {}", payload.paymentReference());
            return ResponseEntity.ok(Response.success("Acknowledged", null));
        }

        Optional<IdempotencyKey> idempotencyKeyRecord = idempotencyKeyRepository.findByPayment(payment);

        if (idempotencyKeyRecord.isEmpty()) {
            log.error("Payment {} exists with no idempotency record. Data integrity issue",
                    payment.getReference());
            return ResponseEntity.ok(Response.success("Acknowledged", null));
        }

        IdempotencyKey record = idempotencyKeyRecord.get();

        if (record.getStatus() == IdempotencyKeyStatus.RESOLVED) {
            log.info("Duplicate webhook for already-terminal payment {}, ignoring",
                    payment.getReference());
            return ResponseEntity.ok(Response.success("Acknowledged", null));
        }

        boolean succeeded = "0".equals(payload.statusCode());

        PaymentStatus oldStatus = payment.getStatus();
        PaymentStatus newStatus = succeeded ? PaymentStatus.CAPTURED : PaymentStatus.FAILED;

        payment.setStatus(newStatus);
        record.setStatus(IdempotencyKeyStatus.RESOLVED);

        paymentStatusHistoryService.record(payment, oldStatus, newStatus, ChangeSource.WEBHOOK);

        try {
            record.setResponse(objectMapper.writeValueAsString(StoredOutcome.from(payload)));
        } catch (Exception e) {
            log.error("Failed to serialize webhook payload for reference {}", payment.getReference(), e);
        }

        paymentRepository.save(payment);
        idempotencyKeyRepository.save(record);

        return ResponseEntity.ok(Response.success("Webhook processed", null));
    }

    private boolean verifyHash(String hash, String amount, String timeStamp, String paymentReference) {
        try {
            String payload = amount + timeStamp + paymentReference + webhookSecret;

            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hashBytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));

            return MessageDigest.isEqual(
                    hashBytes,
                    HexFormat.of().parseHex(hash)
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to create payload hash", e);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}