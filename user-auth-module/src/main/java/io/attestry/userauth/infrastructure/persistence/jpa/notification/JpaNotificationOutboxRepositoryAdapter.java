package io.attestry.userauth.infrastructure.persistence.jpa.notification;

import io.attestry.userauth.application.port.notification.NotificationOutboxOperationsPort;
import io.attestry.userauth.application.port.notification.NotificationOutboxWritePort;
import io.attestry.userauth.infrastructure.persistence.jdbc.outbox.JdbcNotificationOutboxOperations;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class JpaNotificationOutboxRepositoryAdapter {

    private final NotificationOutboxWritePort writePort;
    private final NotificationOutboxOperationsPort operationsPort;
}
