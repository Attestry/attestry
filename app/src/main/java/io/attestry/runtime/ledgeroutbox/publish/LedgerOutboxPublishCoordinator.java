package io.attestry.runtime.ledgeroutbox.publish;

import io.attestry.runtime.ledgeroutbox.model.LedgerOutboxExecutionContext;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class LedgerOutboxPublishCoordinator {

    private final LedgerOutboxBatchPublisher batchPublisher;
    private final LedgerOutboxAttemptFinalizer attemptFinalizer;
    private final LedgerOutboxBacklogMetricsRefresher backlogMetricsRefresher;

    LedgerOutboxPublishCoordinator(
        LedgerOutboxBatchPublisher batchPublisher,
        LedgerOutboxAttemptFinalizer attemptFinalizer,
        LedgerOutboxBacklogMetricsRefresher backlogMetricsRefresher
    ) {
        this.batchPublisher = batchPublisher;
        this.attemptFinalizer = attemptFinalizer;
        this.backlogMetricsRefresher = backlogMetricsRefresher;
    }

    public void publishPendingBatch(Instant now, LedgerOutboxExecutionContext executionContext) {
        attemptFinalizer.finalizeAttempts(batchPublisher.publishPending(now, executionContext), now);
    }

    public void refreshBacklogMetrics(Instant now) {
        backlogMetricsRefresher.refresh(now);
    }
}
