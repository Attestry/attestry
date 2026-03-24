package io.attestry.job.notification;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NotificationOutboxPublisher {

    private final NotificationOutboxCoordinator coordinator;

    public NotificationOutboxPublisher(NotificationOutboxCoordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Scheduled(fixedDelayString = "${app.notification.outbox.publish-interval-ms:5000}")
    @Transactional
    public void publishPending() {
        coordinator.publishPending();
    }
}
