package io.attestry.runtime.notificationoutbox;

import org.springframework.stereotype.Component;

@Component
class NotificationOutboxExecutionContextFactory {

    String createProcessingOwner(Object owner) {
        return "notification-publisher-" + Integer.toHexString(System.identityHashCode(owner));
    }
}
