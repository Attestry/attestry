package io.attestry.userauth.infrastructure.persistence.jpa.entity;

import io.attestry.userauth.domain.membership.model.NotificationOutboxStatus;
import io.attestry.userauth.domain.membership.model.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "notification_outbox")
public class NotificationOutboxJpaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Column(name = "recipient", nullable = false, length = 255)
    private String recipient;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationOutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "processing_started_at")
    private Instant processingStartedAt;

    @Column(name = "processing_owner", length = 100)
    private String processingOwner;

    protected NotificationOutboxJpaEntity() {
    }

    public NotificationOutboxJpaEntity(
        String id,
        NotificationType notificationType,
        String recipient,
        String payload,
        NotificationOutboxStatus status,
        int retryCount,
        String lastError,
        Instant createdAt,
        Instant sentAt,
        Instant nextRetryAt,
        Instant processingStartedAt,
        String processingOwner
    ) {
        this.id = id;
        this.notificationType = notificationType;
        this.recipient = recipient;
        this.payload = payload;
        this.status = status;
        this.retryCount = retryCount;
        this.lastError = lastError;
        this.createdAt = createdAt;
        this.sentAt = sentAt;
        this.nextRetryAt = nextRetryAt;
        this.processingStartedAt = processingStartedAt;
        this.processingOwner = processingOwner;
    }

    public String getId() { return id; }
    public NotificationType getNotificationType() { return notificationType; }
    public String getRecipient() { return recipient; }
    public String getPayload() { return payload; }
    public NotificationOutboxStatus getStatus() { return status; }
    public int getRetryCount() { return retryCount; }
    public String getLastError() { return lastError; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSentAt() { return sentAt; }
    public Instant getNextRetryAt() { return nextRetryAt; }
    public Instant getProcessingStartedAt() { return processingStartedAt; }
    public String getProcessingOwner() { return processingOwner; }
}
