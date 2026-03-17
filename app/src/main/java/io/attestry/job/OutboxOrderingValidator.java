package io.attestry.job;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxOrderingValidator {

    private static final Logger log = LoggerFactory.getLogger(OutboxOrderingValidator.class);

    private final JdbcTemplate jdbcTemplate;
    private final Counter violationCounter;

    public OutboxOrderingValidator(JdbcTemplate jdbcTemplate, MeterRegistry meterRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.violationCounter = Counter.builder("ledger.outbox.ordering.violation.count")
            .description("Number of outbox events published out of order within the same aggregate")
            .register(meterRegistry);
    }

    @Scheduled(cron = "${app.kafka.outbox.ordering-check-cron:0 */5 * * * *}")
    public void checkOrderingViolations() {
        Integer violations = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*) FROM (
                    SELECT e.event_id, e.aggregate_id, e.created_at, e.published_at
                    FROM outbox_event e
                    WHERE e.status = 'PUBLISHED'
                      AND e.event_type IN ('LEDGER_APPEND', 'PROJECTION_UPDATE')
                      AND e.published_at IS NOT NULL
                      AND EXISTS (
                          SELECT 1 FROM outbox_event later
                          WHERE later.aggregate_id = e.aggregate_id
                            AND later.event_type IN ('LEDGER_APPEND', 'PROJECTION_UPDATE')
                            AND later.status = 'PUBLISHED'
                            AND later.created_at > e.created_at
                            AND later.published_at < e.published_at
                      )
                ) AS violations
            """,
            Integer.class
        );
        int count = violations == null ? 0 : violations;
        if (count > 0) {
            violationCounter.increment(count);
            log.warn("outbox ordering violation detected: count={}", count);
        }
    }
}
