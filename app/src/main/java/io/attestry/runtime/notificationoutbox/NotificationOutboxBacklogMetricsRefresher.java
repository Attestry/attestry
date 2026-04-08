package io.attestry.runtime.notificationoutbox;

import io.attestry.userauth.application.port.notification.NotificationOutboxOperationsPort;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
class NotificationOutboxBacklogMetricsRefresher {

    private final NotificationOutboxOperationsPort notificationOutboxOperations;
    private final NotificationOutboxMetrics metrics;

    NotificationOutboxBacklogMetricsRefresher(
        NotificationOutboxOperationsPort notificationOutboxOperations,
        NotificationOutboxMetrics metrics
    ) {
        this.notificationOutboxOperations = notificationOutboxOperations;
        this.metrics = metrics;
    }

    void refresh(Instant now) {
        metrics.setPendingSize(notificationOutboxOperations.countPending());
        metrics.setProcessingSize(notificationOutboxOperations.countProcessing());
        metrics.setFailedSize(notificationOutboxOperations.countFailed());
        metrics.setOldestPendingAgeSeconds(notificationOutboxOperations.findOldestPendingAgeSeconds(now));
        metrics.setOldestProcessingAgeSeconds(notificationOutboxOperations.findOldestProcessingAgeSeconds(now));
    }
}
