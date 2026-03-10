package io.attestry.job;

import io.attestry.userauth.application.port.InvitationNotificationPort;
import io.attestry.userauth.application.port.NotificationOutboxRepositoryPort;
import io.attestry.userauth.domain.membership.model.InvitationNotificationPayload;
import io.attestry.userauth.domain.membership.model.NotificationOutbox;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NotificationOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationOutboxPublisher.class);
    private static final int BATCH_SIZE = 20;
    private static final int MAX_RETRIES = 5;
    private static final Duration BASE_DELAY = Duration.ofSeconds(2);
    private static final Duration MAX_BACKOFF = Duration.ofMinutes(5);

    private final NotificationOutboxRepositoryPort notificationOutboxRepository;
    private final InvitationNotificationPort invitationNotificationPort;
    private final Clock clock;

    public NotificationOutboxPublisher(
        NotificationOutboxRepositoryPort notificationOutboxRepository,
        InvitationNotificationPort invitationNotificationPort,
        Clock clock
    ) {
        this.notificationOutboxRepository = notificationOutboxRepository;
        this.invitationNotificationPort = invitationNotificationPort;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.notification.outbox.publish-interval-ms:5000}")
    @Transactional
    public void publishPending() {
        Instant now = Instant.now(clock);
        List<NotificationOutbox> entries = notificationOutboxRepository.findPendingRetryable(now, BATCH_SIZE);

        for (NotificationOutbox entry : entries) {
            try {
                dispatch(entry);
                entry.markSent(Instant.now(clock));
            } catch (Exception ex) {
                handleFailure(entry, ex, Instant.now(clock));
            }
            notificationOutboxRepository.save(entry);
        }
    }

    private void dispatch(NotificationOutbox entry) {
        switch (entry.notificationType()) {
            case INVITATION -> {
                InvitationNotificationPayload p = (InvitationNotificationPayload) entry.payload();
                invitationNotificationPort.send(
                    new InvitationNotificationPort.InvitationNotification(
                        p.invitationId(), p.tenantId(), p.inviteeEmail()
                    )
                );
            }
        }
    }

    private void handleFailure(NotificationOutbox entry, Exception ex, Instant now) {
        String error = trimError(ex.getMessage());
        if (entry.retryCount() + 1 >= MAX_RETRIES) {
            entry.markPermanentlyFailed(error);
            log.warn("Notification permanently failed: id={}, type={}, recipient={}, error={}",
                entry.id(), entry.notificationType(), entry.recipient(), error);
        } else {
            Instant nextRetryAt = computeNextRetryAt(now, entry.retryCount() + 1);
            entry.markFailed(error, nextRetryAt);
            log.debug("Notification send failed, will retry: id={}, retryCount={}, nextRetryAt={}",
                entry.id(), entry.retryCount(), nextRetryAt);
        }
    }

    private Instant computeNextRetryAt(Instant now, int retryCount) {
        long delaySeconds = BASE_DELAY.toSeconds() * (1L << Math.min(retryCount, 10));
        Duration delay = Duration.ofSeconds(Math.min(delaySeconds, MAX_BACKOFF.toSeconds()));
        return now.plus(delay);
    }

    private String trimError(String message) {
        if (message == null) return null;
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
