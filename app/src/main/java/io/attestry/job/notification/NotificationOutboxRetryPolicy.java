package io.attestry.job.notification;

import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
class NotificationOutboxRetryPolicy {

    private static final int MAX_RETRIES = 5;
    private static final Duration BASE_DELAY = Duration.ofSeconds(2);
    private static final Duration MAX_BACKOFF = Duration.ofMinutes(5);

    boolean isPermanentlyFailed(int nextRetryCount) {
        return nextRetryCount >= MAX_RETRIES;
    }

    Instant computeNextRetryAt(Instant now, int retryCount) {
        long delaySeconds = BASE_DELAY.toSeconds() * (1L << Math.min(retryCount, 10));
        Duration delay = Duration.ofSeconds(Math.min(delaySeconds, MAX_BACKOFF.toSeconds()));
        return now.plus(delay);
    }

    String trimError(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
