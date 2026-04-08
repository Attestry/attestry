package io.attestry.runtime.ledgeroutbox.schedule;

import io.attestry.runtime.ledgeroutbox.model.*;
import io.attestry.runtime.ledgeroutbox.repository.*;
import io.attestry.runtime.ledgeroutbox.publish.*;
import io.attestry.runtime.ledgeroutbox.metrics.*;
import io.attestry.config.AppKafkaProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LedgerOutboxRecoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(LedgerOutboxRecoveryScheduler.class);

    private final AppKafkaProperties kafkaProperties;
    private final Clock clock;
    private final LedgerOutboxJobRepository jobRepository;
    private final Counter recoveredCounter;
    private final Counter processingTimeoutCounter;

    public LedgerOutboxRecoveryScheduler(
        AppKafkaProperties kafkaProperties,
        Clock clock,
        LedgerOutboxJobRepository jobRepository,
        MeterRegistry meterRegistry
    ) {
        this.kafkaProperties = kafkaProperties;
        this.clock = clock;
        this.jobRepository = jobRepository;
        this.recoveredCounter = Counter.builder("ledger.outbox.recovered.count")
            .register(meterRegistry);
        this.processingTimeoutCounter = Counter.builder("ledger.outbox.processing.timeout.count")
            .register(meterRegistry);
    }

    @Scheduled(cron = "${app.kafka.outbox.recovery-cron:*/30 * * * * *}")
    @Transactional
    public void recoverStuckProcessingRows() {
        Instant threshold = Instant.now(clock)
            .minus(kafkaProperties.getOutbox().getProcessingTimeoutSeconds(), ChronoUnit.SECONDS);
        int recovered = jobRepository.recoverTimedOutProcessingEvents(threshold);
        if (recovered > 0) {
            recoveredCounter.increment(recovered);
            processingTimeoutCounter.increment(recovered);
            log.warn("recovered stuck outbox events: count={}, threshold={}", recovered, threshold);
        }
    }
}
