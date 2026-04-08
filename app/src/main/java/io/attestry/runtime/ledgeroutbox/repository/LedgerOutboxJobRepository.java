package io.attestry.runtime.ledgeroutbox.repository;

import io.attestry.runtime.ledgeroutbox.model.*;
import io.attestry.config.AppKafkaProperties;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class LedgerOutboxJobRepository {

    private final JdbcTemplate jdbcTemplate;
    private final AppKafkaProperties kafkaProperties;
    private final LedgerOutboxRetryPolicy retryPolicy;
    private final java.time.Clock clock;

    LedgerOutboxJobRepository(
        JdbcTemplate jdbcTemplate,
        AppKafkaProperties kafkaProperties,
        LedgerOutboxRetryPolicy retryPolicy,
        java.time.Clock clock
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.kafkaProperties = kafkaProperties;
        this.retryPolicy = retryPolicy;
        this.clock = clock;
    }

    public List<OutboxEventRecord> claimReadyEvents(
        Instant now,
        int batchSize,
        String processingOwner
    ) {
        return jdbcTemplate.query(
            """
                WITH blocked_aggregates AS (
                    SELECT DISTINCT aggregate_id
                    FROM outbox_event
                    WHERE event_type IN (?, ?)
                      AND (
                          status = ?
                          OR (status = ? AND retry_count > 0 AND next_retry_at > ?)
                      )
                ),
                claimed AS (
                    SELECT event_id
                    FROM outbox_event
                    WHERE event_type IN (?, ?)
                      AND status = ?
                      AND (next_retry_at IS NULL OR next_retry_at <= ?)
                      AND aggregate_id NOT IN (SELECT aggregate_id FROM blocked_aggregates)
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
            OutboxJobSql.ledgerAppend(),
            OutboxJobSql.projectionUpdate(),
            OutboxStatus.PROCESSING.dbValue(),
            OutboxStatus.PENDING.dbValue(),
            Timestamp.from(now),
            OutboxJobSql.ledgerAppend(),
            OutboxJobSql.projectionUpdate(),
            OutboxStatus.PENDING.dbValue(),
            Timestamp.from(now),
            batchSize,
            OutboxStatus.PROCESSING.dbValue(),
            Timestamp.from(now),
            processingOwner
        );
    }

    public long countByStatus(OutboxStatus status) {
        Long count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM outbox_event
                WHERE event_type IN (?, ?)
                  AND status = ?
            """,
            Long.class,
            OutboxJobSql.ledgerAppend(),
            OutboxJobSql.projectionUpdate(),
            status.dbValue()
        );
        return count == null ? 0L : count;
    }

    public long findOldestPendingOrProcessingAgeSeconds(Instant now) {
        Long oldestAgeSeconds = jdbcTemplate.query(
            """
                SELECT created_at
                FROM outbox_event
                WHERE event_type IN (?, ?)
                  AND status IN (?, ?)
                ORDER BY created_at ASC
                LIMIT 1
            """,
            rs -> rs.next()
                ? Math.max(0L, java.time.Duration.between(rs.getTimestamp("created_at").toInstant(), now).getSeconds())
                : 0L,
            OutboxJobSql.ledgerAppend(),
            OutboxJobSql.projectionUpdate(),
            OutboxStatus.PENDING.dbValue(),
            OutboxStatus.PROCESSING.dbValue()
        );
        return oldestAgeSeconds == null ? 0L : oldestAgeSeconds;
    }

    public int recoverTimedOutProcessingEvents(Instant threshold) {
        return jdbcTemplate.update(
            """
                UPDATE outbox_event
                SET status = ?,
                    processing_started_at = NULL,
                    processing_owner = NULL,
                    next_retry_at = NULL
                WHERE event_type IN (?, ?)
                  AND status = ?
                  AND processing_started_at IS NOT NULL
                  AND processing_started_at < ?
            """,
            OutboxStatus.PENDING.dbValue(),
            OutboxJobSql.ledgerAppend(),
            OutboxJobSql.projectionUpdate(),
            OutboxStatus.PROCESSING.dbValue(),
            Timestamp.from(threshold)
        );
    }

    public int cleanupPublishedBefore(Timestamp thresholdTs, int deleteBatchSize) {
        return jdbcTemplate.update(
            """
                DELETE FROM outbox_event
                WHERE event_id IN (
                    SELECT event_id FROM outbox_event
                    WHERE event_type IN (?, ?)
                      AND status = ?
                      AND published_at IS NOT NULL
                      AND published_at < ?
                    LIMIT ?
                )
            """,
            OutboxJobSql.ledgerAppend(),
            OutboxJobSql.projectionUpdate(),
            OutboxStatus.PUBLISHED.dbValue(),
            thresholdTs,
            deleteBatchSize
        );
    }

    public int countOrderingViolationsSince(Timestamp windowStart) {
        Integer violations = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*) FROM (
                    SELECT e.event_id
                    FROM outbox_event e
                    WHERE e.status = ?
                      AND e.event_type IN (?, ?)
                      AND e.published_at >= ?
                      AND EXISTS (
                          SELECT 1 FROM outbox_event later
                          WHERE later.aggregate_id = e.aggregate_id
                            AND later.event_type IN (?, ?)
                            AND later.status = ?
                            AND later.created_at > e.created_at
                            AND later.published_at < e.published_at
                      )
                ) AS violations
            """,
            Integer.class,
            OutboxStatus.PUBLISHED.dbValue(),
            OutboxJobSql.ledgerAppend(),
            OutboxJobSql.projectionUpdate(),
            windowStart,
            OutboxJobSql.ledgerAppend(),
            OutboxJobSql.projectionUpdate(),
            OutboxStatus.PUBLISHED.dbValue()
        );
        return violations == null ? 0 : violations;
    }

    public void markPublished(List<String> successEventIds) {
        if (successEventIds.isEmpty()) {
            return;
        }

        Timestamp publishedAt = Timestamp.from(Instant.now(clock));
        String inClause = String.join(",", successEventIds.stream().map(id -> "?").toList());
        Object[] params = new Object[successEventIds.size() + 3];
        params[0] = OutboxStatus.PUBLISHED.dbValue();
        params[1] = publishedAt;
        params[2] = OutboxStatus.PROCESSING.dbValue();
        for (int i = 0; i < successEventIds.size(); i++) {
            params[i + 3] = successEventIds.get(i);
        }

        jdbcTemplate.update(
            """
                UPDATE outbox_event
                SET status = ?, last_error = NULL, published_at = ?,
                    next_retry_at = NULL, processing_started_at = NULL, processing_owner = NULL
                WHERE status = ? AND event_id IN (%s)
            """.formatted(inClause),
            params
        );
    }

    public void finalizeFailedAttempts(List<PublishAttempt> failedAttempts, Instant now) {
        for (PublishAttempt attempt : failedAttempts) {
            FinalizeFailureDecision decision = decideFailure(attempt, now);
            jdbcTemplate.update(
                """
                    UPDATE outbox_event
                    SET status = ?, retry_count = ?, last_error = ?, next_retry_at = ?,
                        processing_started_at = NULL, processing_owner = NULL
                    WHERE event_id = ? AND status = ?
                """,
                decision.nextStatus().dbValue(),
                decision.nextRetryCount(),
                decision.lastError(),
                decision.nextRetryAt() == null ? null : Timestamp.from(decision.nextRetryAt()),
                decision.eventId(),
                OutboxStatus.PROCESSING.dbValue()
            );
        }
    }

    private OutboxEventRecord mapRecord(ResultSet rs, int rowNum) throws SQLException {
        return new OutboxEventRecord(
            rs.getString("event_id"),
            OutboxAggregateType.fromDbValue(rs.getString("aggregate_type")),
            rs.getString("aggregate_id"),
            OutboxEventType.fromDbValue(rs.getString("event_type")),
            rs.getString("payload"),
            rs.getInt("retry_count"),
            rs.getTimestamp("created_at").toInstant()
        );
    }

    private FinalizeFailureDecision decideFailure(PublishAttempt attempt, Instant now) {
        OutboxEventRecord event = attempt.event();
        int nextRetryCount = event.retryCount() + 1;
        OutboxStatus nextStatus = nextRetryCount >= kafkaProperties.getOutbox().getMaxRetries()
            ? OutboxStatus.FAILED
            : OutboxStatus.PENDING;
        Instant nextRetryAt = nextStatus == OutboxStatus.PENDING
            ? retryPolicy.computeNextRetryAt(now, nextRetryCount)
            : null;
        return new FinalizeFailureDecision(
            event.eventId(),
            nextStatus,
            nextRetryCount,
            trimError(attempt.error() == null ? null : attempt.error().getMessage()),
            nextRetryAt
        );
    }

    private String trimError(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    private record FinalizeFailureDecision(
        String eventId,
        OutboxStatus nextStatus,
        int nextRetryCount,
        String lastError,
        Instant nextRetryAt
    ) {
    }
}
