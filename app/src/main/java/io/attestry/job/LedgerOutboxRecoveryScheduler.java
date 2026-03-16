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
public class LedgerOutboxRecoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(LedgerOutboxRecoveryScheduler.class);

    private final JdbcTemplate jdbcTemplate;
    private final AppKafkaProperties kafkaProperties;
    private final Clock clock;

    public LedgerOutboxRecoveryScheduler(
        JdbcTemplate jdbcTemplate,
        AppKafkaProperties kafkaProperties,
        Clock clock
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.kafkaProperties = kafkaProperties;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.kafka.outbox.recovery-cron:*/30 * * * * *}")
    @Transactional
    public void recoverStuckProcessingRows() {
        Instant threshold = Instant.now(clock)
            .minus(kafkaProperties.getOutbox().getProcessingTimeoutSeconds(), ChronoUnit.SECONDS);
        int recovered = jdbcTemplate.update(
            """
                UPDATE outbox_event
                SET status = 'PENDING',
                    processing_started_at = NULL,
                    processing_owner = NULL,
                    next_retry_at = NULL
                WHERE event_type IN ('LEDGER_APPEND', 'PROJECTION_UPDATE')
                  AND status = 'PROCESSING'
                  AND processing_started_at IS NOT NULL
                  AND processing_started_at < ?
            """,
            Timestamp.from(threshold)
        );
        if (recovered > 0) {
            log.warn("recovered stuck outbox events: count={}, threshold={}", recovered, threshold);
        }
    }
}
