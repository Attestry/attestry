package io.attestry.userauth.application.port.notification;

import io.attestry.userauth.domain.membership.model.NotificationOutbox;
import java.time.Instant;
import java.util.List;

public interface NotificationOutboxRepositoryPort {

    NotificationOutbox save(NotificationOutbox outbox);

    List<NotificationOutbox> findPendingRetryable(Instant now, int batchSize);
}
