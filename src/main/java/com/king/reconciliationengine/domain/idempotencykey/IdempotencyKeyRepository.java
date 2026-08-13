package com.king.reconciliationengine.domain.idempotencykey;

import com.king.reconciliationengine.domain.idempotencykey.entity.IdempotencyKey;
import com.king.reconciliationengine.domain.idempotencykey.enums.IdempotencyKeyStatus;
import com.king.reconciliationengine.domain.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {
    Optional<IdempotencyKey> findByValue(String value);
    Optional<IdempotencyKey> findByPayment(Payment payment);
    List<IdempotencyKey> findByStatusAndCreatedAtBefore(IdempotencyKeyStatus status, Instant cutoff);
}
