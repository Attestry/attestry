package io.attestry.job.notification;

import io.attestry.userauth.application.port.notification.NotificationOutboxOperationsPort;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NotificationOutboxRecoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationOutboxRecoveryScheduler.class);

    private final NotificationOutboxOperationsPort notificationOutboxOperations;
    private final NotificationOutboxMetrics metrics;
    private final Clock clock;

    public NotificationOutboxRecoveryScheduler(
        NotificationOutboxOperationsPort notificationOutboxOperations,
        NotificationOutboxMetrics metrics,
        Clock clock
    ) {
        this.notificationOutboxOperations = notificationOutboxOperations;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.notification.outbox.recovery-cron:*/30 * * * * *}")
    @Transactional
    public void recoverStuckProcessingRows() {
        Instant threshold = Instant.now(clock).minus(300, ChronoUnit.SECONDS);
        int recovered = notificationOutboxOperations.recoverTimedOutProcessing(threshold);
        if (recovered > 0) {
            metrics.incrementRecoveredCount(recovered);
            metrics.incrementProcessingTimeoutCount(recovered);
            log.warn("recovered stuck notification outbox rows: count={}, threshold={}", recovered, threshold);
        }
        refreshBacklogMetrics();
    }

    private void refreshBacklogMetrics() {
        Instant now = Instant.now(clock);
        metrics.setPendingSize(notificationOutboxOperations.countPending());
        metrics.setProcessingSize(notificationOutboxOperations.countProcessing());
        metrics.setFailedSize(notificationOutboxOperations.countFailed());
        metrics.setOldestPendingAgeSeconds(notificationOutboxOperations.findOldestPendingAgeSeconds(now));
        metrics.setOldestProcessingAgeSeconds(notificationOutboxOperations.findOldestProcessingAgeSeconds(now));
    }
}
