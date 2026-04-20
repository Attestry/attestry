package io.attestry.runtime.notificationoutbox;

import io.attestry.runtime.support.ExponentialBackoffRetryPolicy;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
class NotificationOutboxRetryPolicy {

    private final ExponentialBackoffRetryPolicy delegate = new ExponentialBackoffRetryPolicy(
        Duration.ofSeconds(2), Duration.ofMinutes(5), 5
    );

    boolean isPermanentlyFailed(int nextRetryCount) {
        return delegate.isPermanentlyFailed(nextRetryCount);
    }

    Instant computeNextRetryAt(Instant now, int retryCount) {
        return delegate.computeNextRetryAt(now, retryCount);
    }

    String trimError(String message) {
        return delegate.trimError(message);
    }
}
