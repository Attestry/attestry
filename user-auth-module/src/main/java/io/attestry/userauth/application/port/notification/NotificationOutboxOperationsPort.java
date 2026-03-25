package io.attestry.userauth.application.port.notification;

import io.attestry.userauth.domain.membership.model.NotificationOutbox;
import java.time.Instant;
import java.util.List;

public interface NotificationOutboxOperationsPort {

    List<NotificationOutbox> claimPendingRetryable(Instant now, int batchSize, String processingOwner);

    int recoverTimedOutProcessing(Instant threshold);

    long countPending();

    long countProcessing();

    long countFailed();

    long findOldestPendingAgeSeconds(Instant now);

    long findOldestProcessingAgeSeconds(Instant now);
}
