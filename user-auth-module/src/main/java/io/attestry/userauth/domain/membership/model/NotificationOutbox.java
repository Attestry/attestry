package io.attestry.userauth.domain.membership.model;

import java.time.Instant;
import java.util.UUID;

public class NotificationOutbox {

    private final String id;
    private final NotificationType notificationType;
    private final String recipient;
    private final Object payload;
    private NotificationOutboxStatus status;
    private int retryCount;
    private String lastError;
    private final Instant createdAt;
    private Instant sentAt;
    private Instant nextRetryAt;

    private NotificationOutbox(
        String id,
        NotificationType notificationType,
        String recipient,
        Object payload,
        NotificationOutboxStatus status,
        int retryCount,
        String lastError,
        Instant createdAt,
        Instant sentAt,
        Instant nextRetryAt
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
    }

    public static NotificationOutbox create(
        NotificationType notificationType,
        String recipient,
        Object payload,
        Instant now
    ) {
        return new NotificationOutbox(
            UUID.randomUUID().toString(),
            notificationType,
            recipient,
            payload,
            NotificationOutboxStatus.PENDING,
            0,
            null,
            now,
            null,
            null
        );
    }

    public static NotificationOutbox reconstitute(
        String id,
        NotificationType notificationType,
        String recipient,
        Object payload,
        NotificationOutboxStatus status,
        int retryCount,
        String lastError,
        Instant createdAt,
        Instant sentAt,
        Instant nextRetryAt
    ) {
        return new NotificationOutbox(
            id, notificationType, recipient, payload,
            status, retryCount, lastError, createdAt, sentAt, nextRetryAt
        );
    }

    public void markSent(Instant now) {
        this.status = NotificationOutboxStatus.SENT;
        this.sentAt = now;
        this.nextRetryAt = null;
    }

    public void markFailed(String error, Instant nextRetryAt) {
        this.retryCount++;
        this.lastError = error;
        this.nextRetryAt = nextRetryAt;
    }

    public void markPermanentlyFailed(String error) {
        this.retryCount++;
        this.status = NotificationOutboxStatus.FAILED;
        this.lastError = error;
        this.nextRetryAt = null;
    }

    public String id() { return id; }
    public NotificationType notificationType() { return notificationType; }
    public String recipient() { return recipient; }
    public Object payload() { return payload; }
    public NotificationOutboxStatus status() { return status; }
    public int retryCount() { return retryCount; }
    public String lastError() { return lastError; }
    public Instant createdAt() { return createdAt; }
    public Instant sentAt() { return sentAt; }
    public Instant nextRetryAt() { return nextRetryAt; }
}
