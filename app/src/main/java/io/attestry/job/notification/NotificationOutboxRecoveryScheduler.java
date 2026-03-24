package io.attestry.job.notification;

import io.attestry.userauth.application.port.notification.NotificationOutboxRepositoryPort;
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

    private final NotificationOutboxRepositoryPort notificationOutboxRepository;
    private final NotificationOutboxMetrics metrics;
    private final Clock clock;

    public NotificationOutboxRecoveryScheduler(
        NotificationOutboxRepositoryPort notificationOutboxRepository,
        NotificationOutboxMetrics metrics,
        Clock clock
    ) {
        this.notificationOutboxRepository = notificationOutboxRepository;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.notification.outbox.recovery-cron:*/30 * * * * *}")
    @Transactional
    public void recoverStuckProcessingRows() {
        Instant threshold = Instant.now(clock).minus(300, ChronoUnit.SECONDS);
        int recovered = notificationOutboxRepository.recoverTimedOutProcessing(threshold);
        if (recovered > 0) {
            metrics.incrementRecoveredCount(recovered);
            metrics.incrementProcessingTimeoutCount(recovered);
            log.warn("recovered stuck notification outbox rows: count={}, threshold={}", recovered, threshold);
        }
        refreshBacklogMetrics();
    }

    private void refreshBacklogMetrics() {
        Instant now = Instant.now(clock);
        metrics.setPendingSize(notificationOutboxRepository.countPending());
        metrics.setProcessingSize(notificationOutboxRepository.countProcessing());
        metrics.setFailedSize(notificationOutboxRepository.countFailed());
        metrics.setOldestPendingAgeSeconds(notificationOutboxRepository.findOldestPendingAgeSeconds(now));
        metrics.setOldestProcessingAgeSeconds(notificationOutboxRepository.findOldestProcessingAgeSeconds(now));
    }
}
