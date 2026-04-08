package io.attestry.runtime.ledgeroutbox.publish;

import io.attestry.config.AppKafkaProperties;
import io.attestry.runtime.ledgeroutbox.metrics.LedgerOutboxMetrics;
import io.attestry.runtime.ledgeroutbox.model.OutboxPublishSummary;
import io.attestry.runtime.ledgeroutbox.model.PublishAttempt;
import io.attestry.runtime.ledgeroutbox.repository.LedgerOutboxJobRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
class LedgerOutboxAttemptFinalizer {

    private static final Logger log = LoggerFactory.getLogger(LedgerOutboxAttemptFinalizer.class);

    private final AppKafkaProperties kafkaProperties;
    private final TransactionTemplate transactionTemplate;
    private final LedgerOutboxJobRepository jobRepository;
    private final LedgerOutboxMetrics metrics;

    LedgerOutboxAttemptFinalizer(
        AppKafkaProperties kafkaProperties,
        TransactionTemplate transactionTemplate,
        LedgerOutboxJobRepository jobRepository,
        LedgerOutboxMetrics metrics
    ) {
        this.kafkaProperties = kafkaProperties;
        this.transactionTemplate = transactionTemplate;
        this.jobRepository = jobRepository;
        this.metrics = metrics;
    }

    void finalizeAttempts(OutboxPublishSummary summary, Instant now) {
        if (summary.totalAttempts() == 0) {
            return;
        }

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
