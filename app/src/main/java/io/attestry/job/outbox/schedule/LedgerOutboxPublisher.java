package io.attestry.job.outbox.schedule;

import io.attestry.job.outbox.model.*;
import io.attestry.job.outbox.repository.*;
import io.attestry.job.outbox.publish.*;
import io.attestry.job.outbox.metrics.*;
import java.time.Clock;
import java.time.Instant;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LedgerOutboxPublisher {

    private final Clock clock;
    private final LedgerOutboxPublishCoordinator publishCoordinator;
    private final LedgerOutboxMetrics metrics;
    private final LedgerOutboxExecutionContext executionContext;

    public LedgerOutboxPublisher(
        Clock clock,
        LedgerOutboxPublishCoordinator publishCoordinator,
        LedgerOutboxMetrics metrics,
        LedgerOutboxExecutionContextFactory executionContextFactory
    ) {
        this.clock = clock;
        this.publishCoordinator = publishCoordinator;
        this.metrics = metrics;
        this.executionContext = executionContextFactory.createFor(this);
    }

    @Scheduled(fixedDelayString = "${app.kafka.outbox.publish-interval-ms:1000}")
    public void publishPending() {
        Instant now = Instant.now(clock);
        publishCoordinator.refreshBacklogMetrics(now);
        metrics.recordBatch(() -> publishCoordinator.publishPendingBatch(now, executionContext));
        publishCoordinator.refreshBacklogMetrics(Instant.now(clock));
    }
}
