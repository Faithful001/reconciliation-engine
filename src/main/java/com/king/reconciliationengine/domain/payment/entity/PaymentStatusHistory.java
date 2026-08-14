package com.king.reconciliationengine.domain.payment.entity;

import com.king.reconciliationengine.domain.payment.enums.ChangeSource;
import com.king.reconciliationengine.domain.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_status_history")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class PaymentStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    private PaymentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus toStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChangeSource source;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    @PrePersist
    public void prePersist() {
        occurredAt = Instant.now();
    }
}
