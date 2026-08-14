package com.king.reconciliationengine.domain.reconciliation;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReconciliationScheduler {

    private final ReconciliationService reconciliationService;

    @Scheduled(fixedDelay = 5 * 60 * 1000) // 5 minutes
    @SchedulerLock(name = "reconciliation", lockAtLeastFor = "PT1M")
    public void run() {
        reconciliationService.reconcile();
    }
}