package io.attestry.ledgerservice.outbox;

import io.attestry.ledgerservice.outbox.persistence.LedgerOutboxEventJpaRepository;
import io.attestry.ledgerservice.outbox.persistence.LedgerOutboxStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class LedgerOutboxCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(LedgerOutboxCleanupScheduler.class);

    private final LedgerOutboxEventJpaRepository repository;
    private final Clock clock;
    private final Duration retentionDuration;

    public LedgerOutboxCleanupScheduler(
        LedgerOutboxEventJpaRepository repository,
        Clock clock,
        @Value("${app.ledger.outbox.cleanup-retention-days:7}") int retentionDays
    ) {
        this.repository = repository;
        this.clock = clock;
        this.retentionDuration = Duration.ofDays(retentionDays);
    }

    @Scheduled(cron = "${app.ledger.outbox.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void cleanup() {
        Instant cutoff = Instant.now(clock).minus(retentionDuration);
        int deleted = repository.deleteByEventTypeAndStatusAndPublishedBefore(
            "LEDGER_APPEND",
            LedgerOutboxStatus.PUBLISHED,
            cutoff
        );
        if (deleted > 0) {
            log.info("ledger outbox cleanup: deleted {} published events older than {}", deleted, cutoff);
        }
    }
}
