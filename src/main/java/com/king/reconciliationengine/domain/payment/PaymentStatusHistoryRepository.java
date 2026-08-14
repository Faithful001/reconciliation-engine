package com.king.reconciliationengine.domain.payment;

import com.king.reconciliationengine.domain.payment.entity.Payment;
import com.king.reconciliationengine.domain.payment.entity.PaymentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentStatusHistoryRepository extends JpaRepository<PaymentStatusHistory, UUID> {
    List<PaymentStatusHistory> findByPaymentOrderByOccurredAtAsc(Payment payment);
}
