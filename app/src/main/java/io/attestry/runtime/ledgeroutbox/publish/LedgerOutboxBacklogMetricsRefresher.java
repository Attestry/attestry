package io.attestry.runtime.ledgeroutbox.publish;

import io.attestry.runtime.ledgeroutbox.metrics.LedgerOutboxMetrics;
import io.attestry.runtime.ledgeroutbox.model.OutboxStatus;
import io.attestry.runtime.ledgeroutbox.repository.LedgerOutboxJobRepository;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
class LedgerOutboxBacklogMetricsRefresher {

    private final LedgerOutboxJobRepository jobRepository;
    private final LedgerOutboxMetrics metrics;

    LedgerOutboxBacklogMetricsRefresher(
        LedgerOutboxJobRepository jobRepository,
        LedgerOutboxMetrics metrics
    ) {
        this.jobRepository = jobRepository;
        this.metrics = metrics;
    }

    void refresh(Instant now) {
        metrics.setPendingSize(jobRepository.countByStatus(OutboxStatus.PENDING));
        metrics.setProcessingSize(jobRepository.countByStatus(OutboxStatus.PROCESSING));
        metrics.setFailedSize(jobRepository.countByStatus(OutboxStatus.FAILED));
        metrics.setOldestPendingAgeSeconds(jobRepository.findOldestPendingOrProcessingAgeSeconds(now));
    }
}
