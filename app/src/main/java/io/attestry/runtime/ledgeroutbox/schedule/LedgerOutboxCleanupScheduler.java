package io.attestry.runtime.ledgeroutbox.schedule;

import io.attestry.runtime.ledgeroutbox.model.*;
import io.attestry.runtime.ledgeroutbox.repository.*;
import io.attestry.runtime.ledgeroutbox.publish.*;
import io.attestry.runtime.ledgeroutbox.metrics.*;
import io.attestry.config.AppKafkaProperties;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LedgerOutboxCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(LedgerOutboxCleanupScheduler.class);
    private static final int DELETE_BATCH_SIZE = 1000;

    private final AppKafkaProperties kafkaProperties;
    private final Clock clock;
    private final LedgerOutboxJobRepository jobRepository;
    private final TransactionTemplate transactionTemplate;

    public LedgerOutboxCleanupScheduler(
        AppKafkaProperties kafkaProperties,
        Clock clock,
        LedgerOutboxJobRepository jobRepository,
        TransactionTemplate transactionTemplate
    ) {
        this.kafkaProperties = kafkaProperties;
        this.clock = clock;
        this.jobRepository = jobRepository;
        this.transactionTemplate = transactionTemplate;
    }

    @Scheduled(cron = "${app.kafka.outbox.cleanup-cron:0 0 3 * * *}")
    public void cleanupPublished() {
        Instant threshold = Instant.now(clock)
            .minus(kafkaProperties.getOutbox().getCleanupRetentionDays(), ChronoUnit.DAYS);
        Timestamp thresholdTs = Timestamp.from(threshold);

        int totalDeleted = 0;
        int deleted;
        do {
            Integer result = transactionTemplate.execute(status ->
                jobRepository.cleanupPublishedBefore(thresholdTs, DELETE_BATCH_SIZE));
            deleted = result == null ? 0 : result;
            totalDeleted += deleted;
        } while (deleted >= DELETE_BATCH_SIZE);

        if (totalDeleted > 0) {
            log.info("cleaned up ledger outbox events: deleted={}, threshold={}", totalDeleted, threshold);
        }
    }
}
