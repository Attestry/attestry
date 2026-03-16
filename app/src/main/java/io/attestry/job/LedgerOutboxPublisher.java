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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
    private static final String PENDING_STATUS = "PENDING";
    private static final String PROCESSING_STATUS = "PROCESSING";
    private static final String PUBLISHED_STATUS = "PUBLISHED";
    private static final String FAILED_STATUS = "FAILED";

    private final JdbcTemplate jdbcTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AppKafkaProperties kafkaProperties;
    private final Clock clock;
    private final Counter claimCounter;
    private final Counter publishCounter;
    private final Counter publishSuccessCounter;
    private final Counter publishFailureCounter;
    private final Counter publishSuccessStandaloneCounter;
    private final Counter publishFailureStandaloneCounter;
    private final Timer claimTimer;
    private final Timer publishTimer;
    private final Timer finalizeTimer;
    private final Timer batchTimer;
    private final AtomicLong pendingSizeGauge;
    private final AtomicLong processingSizeGauge;
    private final AtomicLong failedSizeGauge;
    private final AtomicLong oldestPendingAgeSecondsGauge;
    private final String processingOwner;

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
        this.claimCounter = Counter.builder("ledger.outbox.claim.count")
            .register(meterRegistry);
        this.publishCounter = Counter.builder("ledger.outbox.publish.count")
            .register(meterRegistry);
        this.publishSuccessCounter = Counter.builder("ledger.outbox.publish.count")
            .tag("result", "success")
            .register(meterRegistry);
        this.publishFailureCounter = Counter.builder("ledger.outbox.publish.count")
            .tag("result", "failure")
            .register(meterRegistry);
        this.publishSuccessStandaloneCounter = Counter.builder("ledger.outbox.publish.success.count")
            .register(meterRegistry);
        this.publishFailureStandaloneCounter = Counter.builder("ledger.outbox.publish.failure.count")
            .register(meterRegistry);
        this.claimTimer = Timer.builder("ledger.outbox.claim.duration")
            .register(meterRegistry);
        this.publishTimer = Timer.builder("ledger.outbox.publish.duration")
            .register(meterRegistry);
        this.finalizeTimer = Timer.builder("ledger.outbox.finalize.duration")
            .register(meterRegistry);
        this.batchTimer = Timer.builder("ledger.outbox.batch.duration")
            .register(meterRegistry);
        this.pendingSizeGauge = meterRegistry.gauge("ledger.outbox.pending.size", new AtomicLong(0));
        this.processingSizeGauge = meterRegistry.gauge("ledger.outbox.processing.size", new AtomicLong(0));
        this.failedSizeGauge = meterRegistry.gauge("ledger.outbox.failed.size", new AtomicLong(0));
        this.oldestPendingAgeSecondsGauge = meterRegistry.gauge("ledger.outbox.pending.oldest.age", new AtomicLong(0));
        this.processingOwner = "publisher-" + Integer.toHexString(System.identityHashCode(this));
    }

    @Scheduled(fixedDelayString = "${app.kafka.outbox.publish-interval-ms:1000}")
    @Transactional
    public void publishPending() {
        refreshBacklogMetrics();
        batchTimer.record(this::doPublishPending);
        refreshBacklogMetrics();
    }

    private void doPublishPending() {
        Instant now = Instant.now(clock);
        List<OutboxEventRecord> pending = claimTimer.record(() -> jdbcTemplate.query(
            """
                WITH claimed AS (
                    SELECT event_id
                    FROM outbox_event
                    WHERE event_type IN ('LEDGER_APPEND', 'PROJECTION_UPDATE')
                      AND status = ?
                      AND (next_retry_at IS NULL OR next_retry_at <= ?)
                    ORDER BY created_at ASC
                    LIMIT ?
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE outbox_event target
                SET status = ?,
                    processing_started_at = ?,
                    processing_owner = ?,
                    last_error = NULL
                FROM claimed
                WHERE target.event_id = claimed.event_id
                RETURNING target.event_id, target.aggregate_type, target.aggregate_id, target.event_type,
                          target.payload, target.retry_count, target.created_at
            """,
            this::mapRecord,
            PENDING_STATUS,
            Timestamp.from(now),
            Math.max(1, kafkaProperties.getOutbox().getBatchSize()),
            PROCESSING_STATUS,
            Timestamp.from(now),
            processingOwner
        ));
        claimCounter.increment(pending.size());

        Map<String, List<OutboxEventRecord>> groupedByAggregate = groupByAggregateId(pending);
        List<CompletableFuture<List<PublishAttempt>>> groupFutures = groupedByAggregate.values().stream()
            .map(events -> CompletableFuture.supplyAsync(() -> publishGroupSequentially(events)))
            .toList();

        List<PublishAttempt> attempts = groupFutures.stream()
            .map(CompletableFuture::join)
            .flatMap(List::stream)
            .toList();

        for (PublishAttempt attempt : attempts) {
            OutboxEventRecord event = attempt.event();
            publishCounter.increment();
            try {
                if (attempt.error() != null) {
                    throw attempt.error();
                }
                finalizeTimer.record(() -> jdbcTemplate.update(
                    """
                        UPDATE outbox_event
                        SET status = ?, last_error = ?, published_at = ?, next_retry_at = ?,
                            processing_started_at = ?, processing_owner = ?
                        WHERE event_id = ? AND status = ?
                    """,
                    PUBLISHED_STATUS,
                    null,
                    Timestamp.from(Instant.now(clock)),
                    null,
                    null,
                    null,
                    event.eventId(),
                    PROCESSING_STATUS
                ));
                publishSuccessCounter.increment();
                publishSuccessStandaloneCounter.increment();
            } catch (Throwable ex) {
                int nextRetryCount = event.retryCount() + 1;
                String nextStatus = nextRetryCount >= kafkaProperties.getOutbox().getMaxRetries()
                    ? FAILED_STATUS
                    : PENDING_STATUS;
                Instant nextRetryAt = PENDING_STATUS.equals(nextStatus)
                    ? computeNextRetryAt(now, nextRetryCount)
                    : null;
                finalizeTimer.record(() -> jdbcTemplate.update(
                    """
                        UPDATE outbox_event
                        SET status = ?, retry_count = ?, last_error = ?, next_retry_at = ?,
                            processing_started_at = ?, processing_owner = ?
                        WHERE event_id = ? AND status = ?
                    """,
                    nextStatus,
                    nextRetryCount,
                    trimError(ex.getMessage()),
                    nextRetryAt == null ? null : Timestamp.from(nextRetryAt),
                    null,
                    null,
                    event.eventId(),
                    PROCESSING_STATUS
                ));
                publishFailureCounter.increment();
                publishFailureStandaloneCounter.increment();
                if (FAILED_STATUS.equals(nextStatus)) {
                    log.warn("ledger outbox event permanently failed: eventId={}, retryCount={}, lastError={}",
                        event.eventId(), nextRetryCount, trimError(ex.getMessage()));
                }
            }
        }
    }

    private Map<String, List<OutboxEventRecord>> groupByAggregateId(List<OutboxEventRecord> events) {
        Map<String, List<OutboxEventRecord>> grouped = new LinkedHashMap<>();
        for (OutboxEventRecord event : events) {
            grouped.computeIfAbsent(event.aggregateId(), ignored -> new ArrayList<>()).add(event);
        }
        return grouped;
    }

    private List<PublishAttempt> publishGroupSequentially(List<OutboxEventRecord> events) {
        List<PublishAttempt> attempts = new ArrayList<>(events.size());
        for (OutboxEventRecord event : events) {
            try {
                publishTimer.record(() -> publishToTopicsAsync(event).join());
                attempts.add(new PublishAttempt(event, null));
            } catch (Throwable ex) {
                attempts.add(new PublishAttempt(event, ex));
            }
        }
        return attempts;
    }

    private CompletableFuture<Void> publishToTopicsAsync(OutboxEventRecord event) {
        if ("LEDGER_APPEND".equals(event.eventType())) {
            CompletableFuture<?> ledgerFuture = kafkaTemplate.send(
                kafkaProperties.getTopics().getLedgerOutbox(),
                event.aggregateId(),
                event.payload()
            );

            if ("PRODUCT".equals(event.aggregateType())
                || "SHIPMENT".equals(event.aggregateType())
                || "TRANSFER".equals(event.aggregateType())) {
                CompletableFuture<?> projectionFuture = kafkaTemplate.send(
                    kafkaProperties.getTopics().getProductProjection(),
                    event.aggregateId(),
                    event.payload()
                );
                return CompletableFuture.allOf(ledgerFuture, projectionFuture);
            }
            return CompletableFuture.allOf(ledgerFuture);
        }

        if ("PROJECTION_UPDATE".equals(event.eventType())) {
            CompletableFuture<?> projectionFuture = kafkaTemplate.send(
                kafkaProperties.getTopics().getProductProjection(),
                event.aggregateId(),
                event.payload()
            );
            return CompletableFuture.allOf(projectionFuture);
        }
        return CompletableFuture.completedFuture(null);
    }

    private void refreshBacklogMetrics() {
        Instant now = Instant.now(clock);
        pendingSizeGauge.set(countByStatus(PENDING_STATUS));
        processingSizeGauge.set(countByStatus(PROCESSING_STATUS));
        failedSizeGauge.set(countByStatus(FAILED_STATUS));
        Long oldestAgeSeconds = jdbcTemplate.query(
            """
                SELECT created_at
                FROM outbox_event
                WHERE event_type IN ('LEDGER_APPEND', 'PROJECTION_UPDATE')
                  AND status = ?
                ORDER BY created_at ASC
                LIMIT 1
            """,
            rs -> rs.next()
                ? Math.max(0L, Duration.between(rs.getTimestamp("created_at").toInstant(), now).getSeconds())
                : 0L,
            PENDING_STATUS
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

    private record PublishAttempt(OutboxEventRecord event, Throwable error) {
    }
}
