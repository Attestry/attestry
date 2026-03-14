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
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LedgerOutboxCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(LedgerOutboxCleanupScheduler.class);

    private final JdbcTemplate jdbcTemplate;
    private final AppKafkaProperties kafkaProperties;
    private final Clock clock;

    public LedgerOutboxCleanupScheduler(
        JdbcTemplate jdbcTemplate,
        AppKafkaProperties kafkaProperties,
        Clock clock
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.kafkaProperties = kafkaProperties;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.kafka.outbox.cleanup-cron:0 0 3 * * *}")
    @Transactional
    public void cleanupPublished() {
        Instant threshold = Instant.now(clock)
            .minus(kafkaProperties.getOutbox().getCleanupRetentionDays(), ChronoUnit.DAYS);
        int deleted = jdbcTemplate.update(
            """
                DELETE FROM outbox_event
                WHERE event_type IN ('LEDGER_APPEND', 'PROJECTION_UPDATE')
                  AND status = 'PUBLISHED'
                  AND published_at IS NOT NULL
                  AND published_at < ?
            """,
            Timestamp.from(threshold)
        );
        if (deleted > 0) {
            log.info("cleaned up ledger outbox events: deleted={}, threshold={}", deleted, threshold);
        }
    }
}
