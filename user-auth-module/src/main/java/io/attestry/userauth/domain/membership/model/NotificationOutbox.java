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
    private Instant processingStartedAt;
    private String processingOwner;

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
            null,
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
        Instant nextRetryAt,
        Instant processingStartedAt,
        String processingOwner
    ) {
        return new NotificationOutbox(
            id, notificationType, recipient, payload,
            status, retryCount, lastError, createdAt, sentAt, nextRetryAt, processingStartedAt, processingOwner
        );
    }

    public void markProcessing(Instant now, String owner) {
        this.status = NotificationOutboxStatus.PROCESSING;
        this.processingStartedAt = now;
        this.processingOwner = owner;
        this.lastError = null;
    }

    public void markSent(Instant now) {
        this.status = NotificationOutboxStatus.SENT;
        this.sentAt = now;
        this.nextRetryAt = null;
        this.processingStartedAt = null;
        this.processingOwner = null;
    }

    public void markFailed(String error, Instant nextRetryAt) {
        this.retryCount++;
        this.status = NotificationOutboxStatus.PENDING;
        this.lastError = error;
        this.nextRetryAt = nextRetryAt;
        this.processingStartedAt = null;
        this.processingOwner = null;
    }

    public void markPermanentlyFailed(String error) {
        this.retryCount++;
        this.status = NotificationOutboxStatus.FAILED;
        this.lastError = error;
        this.nextRetryAt = null;
        this.processingStartedAt = null;
        this.processingOwner = null;
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
    public Instant processingStartedAt() { return processingStartedAt; }
    public String processingOwner() { return processingOwner; }
}
