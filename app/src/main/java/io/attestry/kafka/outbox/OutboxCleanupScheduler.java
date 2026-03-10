package io.attestry.kafka.outbox;

import io.attestry.kafka.outbox.persistence.OutboxEventJpaRepository;
import io.attestry.kafka.outbox.persistence.OutboxStatus;
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
public class OutboxCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxCleanupScheduler.class);

    private final OutboxEventJpaRepository repository;
    private final Clock clock;
    private final Duration retentionDuration;

    public OutboxCleanupScheduler(
        OutboxEventJpaRepository repository,
        Clock clock,
        @Value("${app.kafka.outbox.cleanup-retention-days:7}") int retentionDays
    ) {
        this.repository = repository;
        this.clock = clock;
        this.retentionDuration = Duration.ofDays(retentionDays);
    }

    @Scheduled(cron = "${app.kafka.outbox.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void cleanup() {
        Instant cutoff = Instant.now(clock).minus(retentionDuration);
        int deleted = repository.deleteByStatusAndPublishedBefore(OutboxStatus.PUBLISHED, cutoff);
        if (deleted > 0) {
            log.info("outbox cleanup: deleted {} published events older than {}", deleted, cutoff);
        }
    }
}
