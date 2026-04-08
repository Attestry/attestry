package io.attestry.runtime.ledgeroutbox.publish;

import io.attestry.config.AppKafkaProperties;
import io.attestry.runtime.ledgeroutbox.metrics.LedgerOutboxMetrics;
import io.attestry.runtime.ledgeroutbox.model.LedgerOutboxExecutionContext;
import io.attestry.runtime.ledgeroutbox.model.OutboxEventRecord;
import io.attestry.runtime.ledgeroutbox.model.OutboxPublishGroups;
import io.attestry.runtime.ledgeroutbox.model.OutboxPublishSummary;
import io.attestry.runtime.ledgeroutbox.model.PublishAttempt;
import io.attestry.runtime.ledgeroutbox.repository.LedgerOutboxJobRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
class LedgerOutboxBatchPublisher {

    private final AppKafkaProperties kafkaProperties;
    private final LedgerOutboxJobRepository jobRepository;
    private final LedgerOutboxTopicPublisher topicPublisher;
    private final LedgerOutboxMetrics metrics;
    private final ExecutorService publishExecutor;

    LedgerOutboxBatchPublisher(
        AppKafkaProperties kafkaProperties,
        LedgerOutboxJobRepository jobRepository,
        LedgerOutboxTopicPublisher topicPublisher,
        LedgerOutboxMetrics metrics,
        @Qualifier("ledgerOutboxPublishExecutor") ExecutorService publishExecutor
    ) {
        this.kafkaProperties = kafkaProperties;
        this.jobRepository = jobRepository;
        this.topicPublisher = topicPublisher;
        this.metrics = metrics;
        this.publishExecutor = publishExecutor;
    }

    OutboxPublishSummary publishPending(Instant now, LedgerOutboxExecutionContext executionContext) {
        List<OutboxEventRecord> claimed = claimPending(now, executionContext);
        if (claimed.isEmpty()) {
            return OutboxPublishSummary.from(List.of());
        }
        metrics.incrementClaimCount(claimed.size());

        OutboxPublishGroups publishGroups = OutboxPublishGroups.from(claimed);
        List<CompletableFuture<List<PublishAttempt>>> groupFutures = publishGroups.byAggregateId().values().stream()
            .map(events -> CompletableFuture.supplyAsync(() -> publishGroupSequentially(events), publishExecutor))
            .toList();

        List<PublishAttempt> attempts = groupFutures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .toList();

        return OutboxPublishSummary.from(attempts);
    }

    private List<OutboxEventRecord> claimPending(Instant now, LedgerOutboxExecutionContext executionContext) {
        try {
            return metrics.recordClaim(() -> jobRepository.claimReadyEvents(
                now,
                Math.max(1, kafkaProperties.getOutbox().getBatchSize()),
                executionContext.processingOwner()
            ));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to claim pending outbox events", e);
        }
    }

    private List<PublishAttempt> publishGroupSequentially(List<OutboxEventRecord> events) {
        List<PublishAttempt> attempts = new ArrayList<>(events.size());
        for (OutboxEventRecord event : events) {
            try {
                metrics.recordPublish(() -> topicPublisher.publish(event).join());
                attempts.add(new PublishAttempt(event, null));
            } catch (Throwable ex) {
                attempts.add(new PublishAttempt(event, ex));
            }
        }
        return attempts;
    }
}
