package io.attestry.notification;

import io.attestry.userauth.application.port.notification.InvitationNotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "app.user-auth.invitation.mail",
    name = "provider",
    havingValue = "LOG",
    matchIfMissing = true
)
public class LoggingInvitationNotificationAdapter implements InvitationNotificationPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingInvitationNotificationAdapter.class);

    private final boolean enabled;
    private final String acceptUrlTemplate;

    public LoggingInvitationNotificationAdapter(
        @Value("${app.user-auth.invitation.mail.enabled:true}") boolean enabled,
        @Value("${app.user-auth.invitation.accept-url-template:http://localhost:8080/invitations/%s/accept}") String acceptUrlTemplate
    ) {
        this.enabled = enabled;
        this.acceptUrlTemplate = acceptUrlTemplate;
    }

    @Override
    public void send(InvitationNotification notification) {
        if (!enabled) {
            return;
        }
        String acceptUrl = String.format(acceptUrlTemplate, notification.invitationId());
        log.info(
            "Invitation email prepared. to={}, tenantId={}, invitationId={}, acceptUrl={}",
            notification.inviteeEmail(),
            notification.tenantId(),
            notification.invitationId(),
            acceptUrl
        );
    }
}

