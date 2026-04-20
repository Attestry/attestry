package io.attestry.runtime.support;

import java.time.Duration;
import java.time.Instant;

public class ExponentialBackoffRetryPolicy {

    private final Duration baseDelay;
    private final Duration maxBackoff;
    private final int maxRetries;

    public ExponentialBackoffRetryPolicy(Duration baseDelay, Duration maxBackoff, int maxRetries) {
        this.baseDelay = baseDelay;
        this.maxBackoff = maxBackoff;
        this.maxRetries = maxRetries;
    }

    public Instant computeNextRetryAt(Instant now, int retryCount) {
        long delaySeconds = baseDelay.toSeconds() * (1L << Math.min(retryCount, 10));
        Duration delay = Duration.ofSeconds(Math.min(delaySeconds, maxBackoff.toSeconds()));
        return now.plus(delay);
    }

    public boolean isPermanentlyFailed(int nextRetryCount) {
        return nextRetryCount >= maxRetries;
    }

    public String trimError(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
