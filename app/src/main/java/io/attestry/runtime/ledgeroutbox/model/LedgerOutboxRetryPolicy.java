package io.attestry.runtime.ledgeroutbox.model;

import io.attestry.runtime.support.ExponentialBackoffRetryPolicy;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class LedgerOutboxRetryPolicy {

    private final ExponentialBackoffRetryPolicy delegate = new ExponentialBackoffRetryPolicy(
        Duration.ofSeconds(2), Duration.ofMinutes(5), 10
    );

    public Instant computeNextRetryAt(Instant now, int retryCount) {
        return delegate.computeNextRetryAt(now, retryCount);
    }
}
