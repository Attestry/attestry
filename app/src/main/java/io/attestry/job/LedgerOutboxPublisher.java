package io.attestry.job;

import io.attestry.config.AppKafkaProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LedgerOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(LedgerOutboxPublisher.class);
    private static final Duration MAX_BACKOFF = Duration.ofMinutes(5);
    private static final Duration BASE_DELAY = Duration.ofSeconds(2);

    private final JdbcTemplate jdbcTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AppKafkaProperties kafkaProperties;
    private final Clock clock;
    private final Counter publishSuccessCounter;
    private final Counter publishFailureCounter;
    private final Timer publishTimer;
    private final AtomicLong pendingSizeGauge;
    private final AtomicLong failedSizeGauge;
    private final AtomicLong oldestPendingAgeSecondsGauge;

    public LedgerOutboxPublisher(
        JdbcTemplate jdbcTemplate,
        KafkaTemplate<String, String> kafkaTemplate,
        AppKafkaProperties kafkaProperties,
        Clock clock,
        MeterRegistry meterRegistry
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaProperties = kafkaProperties;
        this.clock = clock;
        this.publishSuccessCounter = Counter.builder("ledger.outbox.publish.count")
            .tag("result", "success")
            .register(meterRegistry);
        this.publishFailureCounter = Counter.builder("ledger.outbox.publish.count")
            .tag("result", "failure")
            .register(meterRegistry);
        this.publishTimer = Timer.builder("ledger.outbox.publish.duration")
            .register(meterRegistry);
        this.pendingSizeGauge = meterRegistry.gauge("ledger.outbox.pending.size", new AtomicLong(0));
        this.failedSizeGauge = meterRegistry.gauge("ledger.outbox.failed.size", new AtomicLong(0));
        this.oldestPendingAgeSecondsGauge = meterRegistry.gauge("ledger.outbox.pending.oldest.age", new AtomicLong(0));
    }

    @Scheduled(fixedDelayString = "${app.kafka.outbox.publish-interval-ms:1000}")
    @Transactional
    public void publishPending() {
        refreshBacklogMetrics();
        publishTimer.record(this::doPublishPending);
        refreshBacklogMetrics();
    }

    private void doPublishPending() {
        Instant now = Instant.now(clock);
        List<OutboxEventRecord> pending = jdbcTemplate.query(
            """
                SELECT event_id, aggregate_type, aggregate_id, event_type, payload,
                       retry_count, created_at
                FROM outbox_event
                WHERE event_type IN ('LEDGER_APPEND', 'PROJECTION_UPDATE')
                  AND status = 'PENDING'
                  AND (next_retry_at IS NULL OR next_retry_at <= ?)
                ORDER BY created_at ASC
                LIMIT ?
            """,
            this::mapRecord,
            Timestamp.from(now),
            Math.max(1, kafkaProperties.getOutbox().getBatchSize())
        );

        for (OutboxEventRecord event : pending) {
            try {
                publishToTopics(event);
                jdbcTemplate.update(
                    """
                        UPDATE outbox_event
                        SET status = ?, last_error = ?, published_at = ?, next_retry_at = ?
                        WHERE event_id = ?
                    """,
                    "PUBLISHED",
                    null,
                    Timestamp.from(Instant.now(clock)),
                    null,
                    event.eventId()
                );
                publishSuccessCounter.increment();
            } catch (Exception ex) {
                int nextRetryCount = event.retryCount() + 1;
                String nextStatus = nextRetryCount >= kafkaProperties.getOutbox().getMaxRetries()
                    ? "FAILED"
                    : "PENDING";
                Instant nextRetryAt = "PENDING".equals(nextStatus)
                    ? computeNextRetryAt(now, nextRetryCount)
                    : null;
                jdbcTemplate.update(
                    """
                        UPDATE outbox_event
                        SET status = ?, retry_count = ?, last_error = ?, next_retry_at = ?
                        WHERE event_id = ?
                    """,
                    nextStatus,
                    nextRetryCount,
                    trimError(ex.getMessage()),
                    nextRetryAt == null ? null : Timestamp.from(nextRetryAt),
                    event.eventId()
                );
                publishFailureCounter.increment();
                if ("FAILED".equals(nextStatus)) {
                    log.warn("ledger outbox event permanently failed: eventId={}, retryCount={}, lastError={}",
                        event.eventId(), nextRetryCount, trimError(ex.getMessage()));
                }
            }
        }
    }

    private void publishToTopics(OutboxEventRecord event) throws Exception {
        if ("LEDGER_APPEND".equals(event.eventType())) {
            kafkaTemplate.send(
                kafkaProperties.getTopics().getLedgerOutbox(),
                event.aggregateId(),
                event.payload()
            ).get();

            if ("PRODUCT".equals(event.aggregateType())
                || "SHIPMENT".equals(event.aggregateType())
                || "TRANSFER".equals(event.aggregateType())) {
                kafkaTemplate.send(
                    kafkaProperties.getTopics().getProductProjection(),
                    event.aggregateId(),
                    event.payload()
                ).get();
            }
        }

        if ("PROJECTION_UPDATE".equals(event.eventType())) {
            kafkaTemplate.send(
                kafkaProperties.getTopics().getProductProjection(),
                event.aggregateId(),
                event.payload()
            ).get();
        }
    }

    private void refreshBacklogMetrics() {
        Instant now = Instant.now(clock);
        pendingSizeGauge.set(countByStatus("PENDING"));
        failedSizeGauge.set(countByStatus("FAILED"));
        Long oldestAgeSeconds = jdbcTemplate.query(
            """
                SELECT created_at
                FROM outbox_event
                WHERE event_type IN ('LEDGER_APPEND', 'PROJECTION_UPDATE')
                  AND status = 'PENDING'
                ORDER BY created_at ASC
                LIMIT 1
            """,
            rs -> rs.next()
                ? Math.max(0L, Duration.between(rs.getTimestamp("created_at").toInstant(), now).getSeconds())
                : 0L
        );
        oldestPendingAgeSecondsGauge.set(oldestAgeSeconds == null ? 0L : oldestAgeSeconds);
    }

    private long countByStatus(String status) {
        Long count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM outbox_event
                WHERE event_type IN ('LEDGER_APPEND', 'PROJECTION_UPDATE')
                  AND status = ?
            """,
            Long.class,
            status
        );
        return count == null ? 0L : count;
    }

    private OutboxEventRecord mapRecord(ResultSet rs, int rowNum) throws SQLException {
        return new OutboxEventRecord(
            rs.getString("event_id"),
            rs.getString("aggregate_type"),
            rs.getString("aggregate_id"),
            rs.getString("event_type"),
            rs.getString("payload"),
            rs.getInt("retry_count"),
            rs.getTimestamp("created_at").toInstant()
        );
    }

    private Instant computeNextRetryAt(Instant now, int retryCount) {
        long delaySeconds = BASE_DELAY.toSeconds() * (1L << Math.min(retryCount, 10));
        Duration delay = Duration.ofSeconds(Math.min(delaySeconds, MAX_BACKOFF.toSeconds()));
        return now.plus(delay);
    }

    private String trimError(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    private record OutboxEventRecord(
        String eventId,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payload,
        int retryCount,
        Instant createdAt
    ) {
    }
}
