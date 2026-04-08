package io.attestry.runtime.notificationoutbox;

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
    private final NotificationOutboxBacklogMetricsRefresher backlogMetricsRefresher;
    private final NotificationOutboxMetrics metrics;
    private final Clock clock;

    public NotificationOutboxRecoveryScheduler(
        NotificationOutboxOperationsPort notificationOutboxOperations,
        NotificationOutboxBacklogMetricsRefresher backlogMetricsRefresher,
        NotificationOutboxMetrics metrics,
        Clock clock
    ) {
        this.notificationOutboxOperations = notificationOutboxOperations;
        this.backlogMetricsRefresher = backlogMetricsRefresher;
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
        backlogMetricsRefresher.refresh(Instant.now(clock));
    }
}
