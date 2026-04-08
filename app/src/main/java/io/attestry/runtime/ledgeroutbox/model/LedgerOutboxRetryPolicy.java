package io.attestry.runtime.ledgeroutbox.model;

import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class LedgerOutboxRetryPolicy {

    private static final Duration MAX_BACKOFF = Duration.ofMinutes(5);
    private static final Duration BASE_DELAY = Duration.ofSeconds(2);

    public Instant computeNextRetryAt(Instant now, int retryCount) {
        long delaySeconds = BASE_DELAY.toSeconds() * (1L << Math.min(retryCount, 10));
        Duration delay = Duration.ofSeconds(Math.min(delaySeconds, MAX_BACKOFF.toSeconds()));
        return now.plus(delay);
    }
}
