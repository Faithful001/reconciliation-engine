package com.king.reconciliationengine.domain.reconciliation;

import com.king.reconciliationengine.domain.idempotencykey.IdempotencyKeyRepository;
import com.king.reconciliationengine.domain.idempotencykey.entity.IdempotencyKey;
import com.king.reconciliationengine.domain.idempotencykey.enums.IdempotencyKeyStatus;
import com.king.reconciliationengine.domain.payment.PaymentRepository;
import com.king.reconciliationengine.domain.payment.PaymentStatusHistoryService;
import com.king.reconciliationengine.domain.payment.entity.Payment;
import com.king.reconciliationengine.domain.payment.enums.ChangeSource;
import com.king.reconciliationengine.domain.payment.enums.PaymentStatus;
import com.king.reconciliationengine.infrastructure.paymentgateway.paga.PagaClient;
import com.king.reconciliationengine.infrastructure.paymentgateway.paga.dto.PagaVerifyRequest;
import com.king.reconciliationengine.infrastructure.paymentgateway.paga.dto.PagaVerifyResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentStatusHistoryService paymentStatusHistoryService;
    private final PagaClient pagaClient;

    @Value("${paga.public-key}")
    private String pagaPublicKey;

    @Value("${paga.auth-header}")
    private String authHeader;

    @Value("${reconciliation.max-attempts}")
    private int maxAttempts;

    private static final Duration STUCK_THRESHOLD = Duration.ofMinutes(15);

    public void reconcile() {
        Instant cutoff = Instant.now().minus(STUCK_THRESHOLD);
        List<IdempotencyKey> stuck = idempotencyKeyRepository
                .findByStatusAndCreatedAtBefore(IdempotencyKeyStatus.PENDING, cutoff);

        for (IdempotencyKey record : stuck) {
            reconcileOne(record);
        }
    }

    private void reconcileOne(IdempotencyKey record) {
        Payment payment = record.getPayment();

        payment.setReconciliationAttempts(payment.getReconciliationAttempts() + 1);

        PagaVerifyRequest verifyRequest = new PagaVerifyRequest(
                payment.getReference(),
                pagaPublicKey,
                payment.getAmount(),
                payment.getCurrency()
        );

        PagaVerifyResponse verifyResponse;
        try {
            verifyResponse = pagaClient.verify(verifyRequest, authHeader);
        } catch (Exception e) {
            log.error("Verify call failed for {}", payment.getReference(), e);
            paymentRepository.save(payment);
            return;
        }

        PaymentStatus oldStatus = payment.getStatus();

        switch (verifyResponse.status_code()) {
            case 0 -> {
                payment.setStatus(PaymentStatus.CAPTURED);
                record.setStatus(IdempotencyKeyStatus.RESOLVED);
                paymentStatusHistoryService.record(payment, oldStatus, PaymentStatus.CAPTURED, ChangeSource.RECONCILIATION);
                log.info("Reconciled {} as CAPTURED via verify (webhook likely dropped)", payment.getReference());
            }
            case 1 -> {
                payment.setStatus(PaymentStatus.FAILED);
                record.setStatus(IdempotencyKeyStatus.RESOLVED);
                paymentStatusHistoryService.record(payment, oldStatus, PaymentStatus.FAILED, ChangeSource.RECONCILIATION);
                log.info("Reconciled {} as FAILED via verify", payment.getReference());
            }
            case 2 -> {
                if (payment.getReconciliationAttempts() >= maxAttempts) {
                    payment.setStatus(PaymentStatus.NEEDS_REVIEW);
                    record.setStatus(IdempotencyKeyStatus.UNKNOWN);
                    paymentStatusHistoryService.record(payment, oldStatus, PaymentStatus.NEEDS_REVIEW, ChangeSource.RECONCILIATION);
                    log.warn("{} still pending after {} attempts — escalated to NEEDS_REVIEW",
                            payment.getReference(), payment.getReconciliationAttempts());
                } else {
                    log.debug("{} still pending on Paga's side (attempt {}/{}), leaving as-is",
                            payment.getReference(), payment.getReconciliationAttempts(), maxAttempts);
                }
            }
            default -> {
                if (payment.getReconciliationAttempts() >= maxAttempts) {
                    payment.setStatus(PaymentStatus.NEEDS_REVIEW);
                    paymentStatusHistoryService.record(payment, oldStatus, PaymentStatus.NEEDS_REVIEW, ChangeSource.RECONCILIATION);
                    log.warn("{} returned ambiguous status_code {} after {} attempts — escalated to NEEDS_REVIEW",
                            payment.getReference(), verifyResponse.status_code(), payment.getReconciliationAttempts());
                }
                record.setStatus(IdempotencyKeyStatus.UNKNOWN);
                log.warn("Unexpected verify status_code {} for {} — needs manual review",
                        verifyResponse.status_code(), payment.getReference());
            }
        }

        paymentRepository.save(payment);
        idempotencyKeyRepository.save(record);
    }
}