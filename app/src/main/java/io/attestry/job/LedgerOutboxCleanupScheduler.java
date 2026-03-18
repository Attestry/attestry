package io.attestry.job;

import io.attestry.config.AppKafkaProperties;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LedgerOutboxCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(LedgerOutboxCleanupScheduler.class);
    private static final int DELETE_BATCH_SIZE = 1000;

    private final JdbcTemplate jdbcTemplate;
    private final AppKafkaProperties kafkaProperties;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public LedgerOutboxCleanupScheduler(
        JdbcTemplate jdbcTemplate,
        AppKafkaProperties kafkaProperties,
        Clock clock,
        TransactionTemplate transactionTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.kafkaProperties = kafkaProperties;
        this.clock = clock;
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
            Integer result = transactionTemplate.execute(status -> jdbcTemplate.update(
                """
                    DELETE FROM outbox_event
                    WHERE event_id IN (
                        SELECT event_id FROM outbox_event
                        WHERE event_type IN ('LEDGER_APPEND', 'PROJECTION_UPDATE')
                          AND status = 'PUBLISHED'
                          AND published_at IS NOT NULL
                          AND published_at < ?
                        LIMIT ?
                    )
                """,
                thresholdTs, DELETE_BATCH_SIZE
            ));
            deleted = result == null ? 0 : result;
            totalDeleted += deleted;
        } while (deleted >= DELETE_BATCH_SIZE);

        if (totalDeleted > 0) {
            log.info("cleaned up ledger outbox events: deleted={}, threshold={}", totalDeleted, threshold);
        }
    }
}
