package io.attestry.userauth.infrastructure.persistence.jpa.notification;

import io.attestry.userauth.application.port.notification.NotificationOutboxWritePort;
import io.attestry.userauth.application.port.notification.NotificationPayloadSerializerPort;
import io.attestry.userauth.domain.membership.model.NotificationOutbox;
import io.attestry.userauth.infrastructure.persistence.jpa.entity.NotificationOutboxJpaEntity;
import io.attestry.userauth.infrastructure.persistence.jpa.repository.NotificationOutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaNotificationOutboxWriteRepository implements NotificationOutboxWritePort {

    private final NotificationOutboxJpaRepository repository;
    private final NotificationPayloadSerializerPort payloadSerializer;

    @Override
    public NotificationOutbox save(NotificationOutbox outbox) {
        NotificationOutboxJpaEntity entity = toEntity(outbox);
        NotificationOutboxJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    public NotificationOutbox toDomain(NotificationOutboxJpaEntity entity) {
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
}
