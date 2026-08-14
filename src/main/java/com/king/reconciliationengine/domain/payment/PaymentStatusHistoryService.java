package com.king.reconciliationengine.domain.payment;

import com.king.reconciliationengine.domain.payment.entity.Payment;
import com.king.reconciliationengine.domain.payment.entity.PaymentStatusHistory;
import com.king.reconciliationengine.domain.payment.enums.ChangeSource;
import com.king.reconciliationengine.domain.payment.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentStatusHistoryService {

    private final PaymentStatusHistoryRepository paymentStatusHistoryRepository;
    
    public void record(Payment payment, PaymentStatus fromStatus, PaymentStatus toStatus, ChangeSource source) {
        PaymentStatusHistory history = PaymentStatusHistory.builder()
                .payment(payment)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .source(source)
                .build();

        paymentStatusHistoryRepository.save(history);
    }
}
