package io.attestry.runtime.notificationoutbox;

import io.attestry.userauth.application.port.notification.NotificationOutboxOperationsPort;
import io.attestry.userauth.application.port.notification.NotificationOutboxWritePort;
import io.attestry.userauth.domain.membership.model.NotificationOutbox;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class NotificationOutboxBatchProcessor {

    private static final int BATCH_SIZE = 20;

    private final NotificationOutboxOperationsPort notificationOutboxOperations;
    private final NotificationOutboxWritePort notificationOutboxWritePort;
    private final NotificationOutboxDispatcher dispatcher;
    private final NotificationOutboxFailureHandler failureHandler;
    private final NotificationOutboxMetrics metrics;
    private final Clock clock;

    NotificationOutboxBatchProcessor(
        NotificationOutboxOperationsPort notificationOutboxOperations,
        NotificationOutboxWritePort notificationOutboxWritePort,
        NotificationOutboxDispatcher dispatcher,
        NotificationOutboxFailureHandler failureHandler,
        NotificationOutboxMetrics metrics,
        Clock clock
    ) {
        this.notificationOutboxOperations = notificationOutboxOperations;
        this.notificationOutboxWritePort = notificationOutboxWritePort;
        this.dispatcher = dispatcher;
        this.failureHandler = failureHandler;
        this.metrics = metrics;
        this.clock = clock;
    }

    void publishPending(String processingOwner) {
        metrics.recordBatch(() -> {
            Instant now = Instant.now(clock);
            List<NotificationOutbox> entries = metrics.recordClaim(
                () -> notificationOutboxOperations.claimPendingRetryable(now, BATCH_SIZE, processingOwner)
            );
            metrics.incrementClaimCount(entries.size());

            for (NotificationOutbox entry : entries) {
                processEntry(entry);
                notificationOutboxWritePort.save(entry);
            }
        });
    }

    private void processEntry(NotificationOutbox entry) {
        try {
            dispatcher.dispatch(entry);
            entry.markSent(Instant.now(clock));
            metrics.incrementPublishSuccessCount();
        } catch (Exception ex) {
            metrics.incrementPublishFailureCount();
            failureHandler.handle(entry, ex, Instant.now(clock));
        }
    }
}
