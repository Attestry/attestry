package io.attestry.job.outbox.publish;

import io.attestry.job.outbox.model.*;
import io.attestry.job.outbox.repository.*;
import io.attestry.job.outbox.metrics.*;
import io.attestry.config.AppKafkaProperties;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class LedgerOutboxPublishCoordinator {

    private static final Logger log = LoggerFactory.getLogger(LedgerOutboxPublishCoordinator.class);

    private final AppKafkaProperties kafkaProperties;
    private final TransactionTemplate transactionTemplate;
    private final LedgerOutboxJobRepository jobRepository;
    private final LedgerOutboxTopicPublisher topicPublisher;
    private final LedgerOutboxMetrics metrics;
    private final ExecutorService publishExecutor;

    LedgerOutboxPublishCoordinator(
        AppKafkaProperties kafkaProperties,
        TransactionTemplate transactionTemplate,
        LedgerOutboxJobRepository jobRepository,
        LedgerOutboxTopicPublisher topicPublisher,
        LedgerOutboxMetrics metrics,
        @Qualifier("ledgerOutboxPublishExecutor") ExecutorService publishExecutor
    ) {
        this.kafkaProperties = kafkaProperties;
        this.transactionTemplate = transactionTemplate;
        this.jobRepository = jobRepository;
        this.topicPublisher = topicPublisher;
        this.metrics = metrics;
        this.publishExecutor = publishExecutor;
    }

    public void publishPendingBatch(Instant now, LedgerOutboxExecutionContext executionContext) {
        List<OutboxEventRecord> claimed = claimPending(now, executionContext);
        if (claimed.isEmpty()) {
            return;
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

        finalizeAttempts(OutboxPublishSummary.from(attempts), now);
    }

    public void refreshBacklogMetrics(Instant now) {
        metrics.setPendingSize(jobRepository.countByStatus(OutboxStatus.PENDING));
        metrics.setProcessingSize(jobRepository.countByStatus(OutboxStatus.PROCESSING));
        metrics.setFailedSize(jobRepository.countByStatus(OutboxStatus.FAILED));
        metrics.setOldestPendingAgeSeconds(jobRepository.findOldestPendingOrProcessingAgeSeconds(now));
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

    private void finalizeAttempts(OutboxPublishSummary summary, Instant now) {
        transactionTemplate.executeWithoutResult(status -> {
            if (!summary.successEventIds().isEmpty()) {
                metrics.recordFinalize(() -> jobRepository.markPublished(summary.successEventIds()));
                metrics.incrementPublishSuccessCount(summary.successEventIds().size());
            }

            for (PublishAttempt attempt : summary.failedAttempts()) {
                metrics.recordFinalize(() -> jobRepository.finalizeFailedAttempts(List.of(attempt), now));
                metrics.incrementPublishFailureCount();
                logPermanentFailureIfNeeded(attempt);
            }

            metrics.incrementPublishCount(summary.totalAttempts());
        });
    }

    private void logPermanentFailureIfNeeded(PublishAttempt attempt) {
        int nextRetryCount = attempt.event().retryCount() + 1;
        if (nextRetryCount >= kafkaProperties.getOutbox().getMaxRetries()) {
            log.warn(
                "ledger outbox event permanently failed: eventId={}, retryCount={}, lastError={}",
                attempt.event().eventId(),
                nextRetryCount,
                trimError(attempt.error() == null ? null : attempt.error().getMessage())
            );
        }
    }

    private String trimError(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
