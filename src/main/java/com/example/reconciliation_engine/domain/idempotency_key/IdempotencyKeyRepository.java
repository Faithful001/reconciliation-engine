package com.example.reconciliation_engine.domain.idempotency_key;

import com.example.reconciliation_engine.domain.idempotency_key.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {

}
