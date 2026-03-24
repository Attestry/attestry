package io.attestry.job.notification;

import io.attestry.userauth.application.port.notification.NotificationOutboxRepositoryPort;
import io.attestry.userauth.domain.membership.model.NotificationOutbox;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class NotificationOutboxCoordinator {

    private static final Logger log = LoggerFactory.getLogger(NotificationOutboxCoordinator.class);
    private static final int BATCH_SIZE = 20;

    private final NotificationOutboxRepositoryPort notificationOutboxRepository;
    private final NotificationOutboxDispatcher dispatcher;
    private final NotificationOutboxRetryPolicy retryPolicy;
    private final NotificationOutboxMetrics metrics;
    private final Clock clock;
    private final String processingOwner;

    NotificationOutboxCoordinator(
        NotificationOutboxRepositoryPort notificationOutboxRepository,
        NotificationOutboxDispatcher dispatcher,
        NotificationOutboxRetryPolicy retryPolicy,
        NotificationOutboxMetrics metrics,
        Clock clock
    ) {
        this.notificationOutboxRepository = notificationOutboxRepository;
        this.dispatcher = dispatcher;
        this.retryPolicy = retryPolicy;
        this.metrics = metrics;
        this.clock = clock;
        this.processingOwner = "notification-publisher-" + Integer.toHexString(System.identityHashCode(this));
    }

    void publishPending() {
        metrics.recordBatch(() -> {
            Instant now = Instant.now(clock);
            List<NotificationOutbox> entries = metrics.recordClaim(
                () -> notificationOutboxRepository.claimPendingRetryable(now, BATCH_SIZE, processingOwner)
            );
            metrics.incrementClaimCount(entries.size());

            for (NotificationOutbox entry : entries) {
                try {
                    dispatcher.dispatch(entry);
                    entry.markSent(Instant.now(clock));
                    metrics.incrementPublishSuccessCount();
                } catch (Exception ex) {
                    metrics.incrementPublishFailureCount();
                    handleFailure(entry, ex, Instant.now(clock));
                }
                notificationOutboxRepository.save(entry);
            }

            refreshBacklogMetrics(Instant.now(clock));
        });
    }

    private void handleFailure(NotificationOutbox entry, Exception ex, Instant now) {
        int nextRetryCount = entry.retryCount() + 1;
        String error = retryPolicy.trimError(ex.getMessage());
        if (retryPolicy.isPermanentlyFailed(nextRetryCount)) {
            entry.markPermanentlyFailed(error);
            log.warn(
                "Notification permanently failed: id={}, type={}, recipient={}, error={}",
                entry.id(),
                entry.notificationType(),
                entry.recipient(),
                error
            );
            return;
        }

        Instant nextRetryAt = retryPolicy.computeNextRetryAt(now, nextRetryCount);
        entry.markFailed(error, nextRetryAt);
        log.debug(
            "Notification send failed, will retry: id={}, retryCount={}, nextRetryAt={}",
            entry.id(),
            entry.retryCount(),
            nextRetryAt
        );
    }

    void refreshBacklogMetrics() {
        refreshBacklogMetrics(Instant.now(clock));
    }

    private void refreshBacklogMetrics(Instant now) {
        metrics.setPendingSize(notificationOutboxRepository.countPending());
        metrics.setProcessingSize(notificationOutboxRepository.countProcessing());
        metrics.setFailedSize(notificationOutboxRepository.countFailed());
        metrics.setOldestPendingAgeSeconds(notificationOutboxRepository.findOldestPendingAgeSeconds(now));
        metrics.setOldestProcessingAgeSeconds(notificationOutboxRepository.findOldestProcessingAgeSeconds(now));
    }
}
