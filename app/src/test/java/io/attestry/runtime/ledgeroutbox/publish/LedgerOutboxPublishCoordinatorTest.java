package io.attestry.runtime.ledgeroutbox.publish;

import static org.mockito.Mockito.verify;

import io.attestry.runtime.ledgeroutbox.model.LedgerOutboxExecutionContext;
import io.attestry.runtime.ledgeroutbox.model.OutboxPublishSummary;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LedgerOutboxPublishCoordinatorTest {

    @Mock private LedgerOutboxBatchPublisher batchPublisher;
    @Mock private LedgerOutboxAttemptFinalizer attemptFinalizer;
    @Mock private LedgerOutboxBacklogMetricsRefresher backlogMetricsRefresher;

    @Test
    void publishPendingBatch_delegatesToBatchPublisherAndFinalizer() {
        LedgerOutboxPublishCoordinator coordinator = new LedgerOutboxPublishCoordinator(
            batchPublisher,
            attemptFinalizer,
            backlogMetricsRefresher
        );
        Instant now = Instant.parse("2026-04-08T10:00:00Z");
        LedgerOutboxExecutionContext context = new LedgerOutboxExecutionContext("publisher-test");
        OutboxPublishSummary summary = new OutboxPublishSummary(List.of("event-1"), List.of(), 1);
        org.mockito.Mockito.when(batchPublisher.publishPending(now, context)).thenReturn(summary);

        coordinator.publishPendingBatch(now, context);

        verify(batchPublisher).publishPending(now, context);
        verify(attemptFinalizer).finalizeAttempts(summary, now);
    }

    @Test
    void refreshBacklogMetrics_delegatesToRefresher() {
        LedgerOutboxPublishCoordinator coordinator = new LedgerOutboxPublishCoordinator(
            batchPublisher,
            attemptFinalizer,
            backlogMetricsRefresher
        );
        Instant now = Instant.parse("2026-04-08T10:00:00Z");

        coordinator.refreshBacklogMetrics(now);

        verify(backlogMetricsRefresher).refresh(now);
    }
}
