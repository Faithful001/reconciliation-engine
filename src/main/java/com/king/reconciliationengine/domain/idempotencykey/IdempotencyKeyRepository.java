package com.king.reconciliationengine.domain.idempotencykey;

import com.king.reconciliationengine.domain.idempotencykey.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {

}
