package io.attestry.runtime.notificationoutbox;

import io.attestry.userauth.domain.membership.model.NotificationOutbox;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class NotificationOutboxFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationOutboxFailureHandler.class);

    private final NotificationOutboxRetryPolicy retryPolicy;

    NotificationOutboxFailureHandler(NotificationOutboxRetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    void handle(NotificationOutbox entry, Exception ex, Instant now) {
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
}
