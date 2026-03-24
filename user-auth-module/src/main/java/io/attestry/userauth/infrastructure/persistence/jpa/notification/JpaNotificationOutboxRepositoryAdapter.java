package io.attestry.userauth.infrastructure.persistence.jpa.notification;

import io.attestry.userauth.application.port.notification.NotificationOutboxRepositoryPort;
import io.attestry.userauth.application.port.notification.NotificationPayloadSerializerPort;
import io.attestry.userauth.domain.membership.model.NotificationOutbox;
import io.attestry.userauth.domain.membership.model.NotificationOutboxStatus;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.NotificationOutboxJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.NotificationOutboxJpaRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaNotificationOutboxRepositoryAdapter implements NotificationOutboxRepositoryPort {

    private final NotificationOutboxJpaRepository repository;
    private final NotificationPayloadSerializerPort payloadSerializer;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public NotificationOutbox save(NotificationOutbox outbox) {
        NotificationOutboxJpaEntity entity = toEntity(outbox);
        NotificationOutboxJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<NotificationOutbox> claimPendingRetryable(Instant now, int batchSize, String processingOwner) {
        List<String> ids = jdbcTemplate.query(
            """
                WITH claimed AS (
                    SELECT id
                    FROM notification_outbox
                    WHERE status = ?
                      AND (next_retry_at IS NULL OR next_retry_at <= ?)
                    ORDER BY created_at ASC
                    LIMIT ?
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE notification_outbox target
                SET status = ?,
                    processing_started_at = ?,
                    processing_owner = ?,
                    last_error = NULL
                FROM claimed
                WHERE target.id = claimed.id
                RETURNING target.id
            """,
            (rs, rowNum) -> rs.getString("id"),
            NotificationOutboxStatus.PENDING.name(),
            Timestamp.from(now),
            batchSize,
            NotificationOutboxStatus.PROCESSING.name(),
            Timestamp.from(now),
            processingOwner
        );
        if (ids.isEmpty()) {
            return List.of();
        }
        return repository.findRetryable(ids).stream().map(this::toDomain).toList();
    }

    @Override
    public int recoverTimedOutProcessing(Instant threshold) {
        return jdbcTemplate.update(
            """
                UPDATE notification_outbox
                SET status = ?,
                    processing_started_at = NULL,
                    processing_owner = NULL,
                    next_retry_at = NULL
                WHERE status = ?
                  AND processing_started_at IS NOT NULL
                  AND processing_started_at < ?
            """,
            NotificationOutboxStatus.PENDING.name(),
            NotificationOutboxStatus.PROCESSING.name(),
            Timestamp.from(threshold)
        );
    }

    @Override
    public long countPending() {
        return countByStatus(NotificationOutboxStatus.PENDING);
    }

    @Override
    public long countProcessing() {
        return countByStatus(NotificationOutboxStatus.PROCESSING);
    }

    @Override
    public long countFailed() {
        return countByStatus(NotificationOutboxStatus.FAILED);
    }

    @Override
    public long findOldestPendingAgeSeconds(Instant now) {
        return findOldestAgeSeconds(
            """
                SELECT created_at
                FROM notification_outbox
                WHERE status = ?
                ORDER BY created_at ASC
                LIMIT 1
            """,
            now,
            NotificationOutboxStatus.PENDING
        );
    }

    @Override
    public long findOldestProcessingAgeSeconds(Instant now) {
        return findOldestAgeSeconds(
            """
                SELECT processing_started_at
                FROM notification_outbox
                WHERE status = ?
                  AND processing_started_at IS NOT NULL
                ORDER BY processing_started_at ASC
                LIMIT 1
            """,
            now,
            NotificationOutboxStatus.PROCESSING
        );
    }

    private long countByStatus(NotificationOutboxStatus status) {
        Long count = jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM notification_outbox
                WHERE status = ?
            """,
            Long.class,
            status.name()
        );
        return count == null ? 0 : count;
    }

    private long findOldestAgeSeconds(String sql, Instant now, NotificationOutboxStatus status) {
        Timestamp oldest = jdbcTemplate.query(
            sql,
            rs -> rs.next() ? rs.getTimestamp(1) : null,
            status.name()
        );
        if (oldest == null) {
            return 0;
        }
        return Math.max(0, now.getEpochSecond() - oldest.toInstant().getEpochSecond());
    }

    private NotificationOutboxJpaEntity toEntity(NotificationOutbox domain) {
        return new NotificationOutboxJpaEntity(
            domain.id(),
            domain.notificationType(),
            domain.recipient(),
            payloadSerializer.serialize(domain.payload()),
            domain.status(),
            domain.retryCount(),
            domain.lastError(),
            domain.createdAt(),
            domain.sentAt(),
            domain.nextRetryAt(),
            domain.processingStartedAt(),
            domain.processingOwner()
        );
    }

    private NotificationOutbox toDomain(NotificationOutboxJpaEntity entity) {
        return NotificationOutbox.reconstitute(
            entity.getId(),
            entity.getNotificationType(),
            entity.getRecipient(),
            payloadSerializer.deserialize(entity.getPayload(), entity.getNotificationType()),
            entity.getStatus(),
            entity.getRetryCount(),
            entity.getLastError(),
            entity.getCreatedAt(),
            entity.getSentAt(),
            entity.getNextRetryAt(),
            entity.getProcessingStartedAt(),
            entity.getProcessingOwner()
        );
    }
}
