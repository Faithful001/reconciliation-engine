package com.king.reconciliationengine.domain.idempotencykey.entity;

import com.king.reconciliationengine.domain.idempotencykey.enums.IdempotencyKeyStatus;
import com.king.reconciliationengine.domain.payment.entity.Payment;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="idempotency_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKey {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String value;

    @JoinColumn(name = "payment_id", nullable = false)
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IdempotencyKeyStatus status;

    @Column(nullable = false)
    private String requestHash;

    @Column(nullable = false, columnDefinition = "jsonb")
    private String response;

    @Column(updatable = false, nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
