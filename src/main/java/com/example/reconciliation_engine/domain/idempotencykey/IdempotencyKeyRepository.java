package com.example.reconciliation_engine.domain.idempotencykey;

import com.example.reconciliation_engine.domain.idempotencykey.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {

}
