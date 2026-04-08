package io.attestry.runtime.notificationoutbox;

import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
class NotificationOutboxCoordinator {

    private final NotificationOutboxBatchProcessor batchProcessor;
    private final NotificationOutboxBacklogMetricsRefresher backlogMetricsRefresher;
    private final String processingOwner;

    NotificationOutboxCoordinator(
        NotificationOutboxBatchProcessor batchProcessor,
        NotificationOutboxBacklogMetricsRefresher backlogMetricsRefresher,
        NotificationOutboxExecutionContextFactory executionContextFactory
    ) {
        this.batchProcessor = batchProcessor;
        this.backlogMetricsRefresher = backlogMetricsRefresher;
        this.processingOwner = executionContextFactory.createProcessingOwner(this);
    }

    void publishPending() {
        batchProcessor.publishPending(processingOwner);
        refreshBacklogMetrics();
    }

    void refreshBacklogMetrics() {
        backlogMetricsRefresher.refresh(Instant.now());
    }
}
