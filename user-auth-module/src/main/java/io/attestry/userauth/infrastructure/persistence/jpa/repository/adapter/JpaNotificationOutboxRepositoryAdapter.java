package io.attestry.userauth.infrastructure.persistence.jpa.repository.adapter;

import io.attestry.userauth.application.port.NotificationOutboxRepositoryPort;
import io.attestry.userauth.application.port.NotificationPayloadSerializerPort;
import io.attestry.userauth.domain.membership.model.NotificationOutbox;
import io.attestry.userauth.domain.membership.model.NotificationOutboxStatus;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.NotificationOutboxJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.NotificationOutboxJpaRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaNotificationOutboxRepositoryAdapter implements NotificationOutboxRepositoryPort {

    private final NotificationOutboxJpaRepository repository;
    private final NotificationPayloadSerializerPort payloadSerializer;

    @Override
    public NotificationOutbox save(NotificationOutbox outbox) {
        NotificationOutboxJpaEntity entity = toEntity(outbox);
        NotificationOutboxJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<NotificationOutbox> findPendingRetryable(Instant now, int batchSize) {
        return repository.findRetryable(
            NotificationOutboxStatus.PENDING,
            now,
            PageRequest.of(0, batchSize)
        ).stream().map(this::toDomain).toList();
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
            domain.nextRetryAt()
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
            entity.getNextRetryAt()
        );
    }
}
