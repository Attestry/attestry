package io.attestry.userauth.application.port.notification;

import io.attestry.userauth.domain.membership.model.NotificationOutbox;

public interface NotificationOutboxWritePort {

    NotificationOutbox save(NotificationOutbox outbox);
}
