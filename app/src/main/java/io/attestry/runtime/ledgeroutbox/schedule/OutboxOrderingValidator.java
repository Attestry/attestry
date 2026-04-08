package io.attestry.runtime.ledgeroutbox.schedule;

import io.attestry.runtime.ledgeroutbox.model.*;
import io.attestry.runtime.ledgeroutbox.repository.*;
import io.attestry.runtime.ledgeroutbox.publish.*;
import io.attestry.runtime.ledgeroutbox.metrics.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxOrderingValidator {

    private static final Logger log = LoggerFactory.getLogger(OutboxOrderingValidator.class);
    private static final Duration CHECK_WINDOW = Duration.ofMinutes(10);

    private final LedgerOutboxJobRepository jobRepository;
    private final Counter violationCounter;
    private final Clock clock;

    public OutboxOrderingValidator(
        LedgerOutboxJobRepository jobRepository,
        MeterRegistry meterRegistry,
        Clock clock
    ) {
        this.jobRepository = jobRepository;
        this.violationCounter = Counter.builder("ledger.outbox.ordering.violation.count")
            .description("Number of outbox events published out of order within the same aggregate")
            .register(meterRegistry);
        this.clock = clock;
    }

    @Scheduled(cron = "${app.kafka.outbox.ordering-check-cron:0 */5 * * * *}")
    public void checkOrderingViolations() {
        Timestamp windowStart = Timestamp.from(Instant.now(clock).minus(CHECK_WINDOW));
        int count = jobRepository.countOrderingViolationsSince(windowStart);
        if (count > 0) {
            violationCounter.increment(count);
            log.warn("outbox ordering violation detected: count={}", count);
        }
    }
}
