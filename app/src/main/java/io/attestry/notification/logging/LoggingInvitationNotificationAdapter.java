package io.attestry.notification.logging;

import io.attestry.notification.InvitationNotificationProperties;
import io.attestry.userauth.application.port.notification.InvitationNotificationPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "app.user-auth.invitation.mail",
    name = "provider",
    havingValue = "LOG",
    matchIfMissing = true
)
public class LoggingInvitationNotificationAdapter implements InvitationNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingInvitationNotificationAdapter.class);

    private final InvitationNotificationProperties properties;

    @Override
    public void send(InvitationNotification notification) {
        if (!properties.getMail().isEnabled()) {
            return;
        }
        String acceptUrl = String.format(properties.getAcceptUrlTemplate(), notification.invitationId());
        log.info(
            "Invitation email prepared. to={}, tenantId={}, invitationId={}, dedupeKey={}, acceptUrl={}",
            notification.inviteeEmail(),
            notification.tenantId(),
            notification.invitationId(),
            notification.dedupeKey(),
            acceptUrl
        );
    }
}
