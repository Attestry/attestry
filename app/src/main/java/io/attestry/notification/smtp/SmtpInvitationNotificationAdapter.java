package io.attestry.notification.smtp;

import io.attestry.notification.InvitationNotificationProperties;
import io.attestry.userauth.application.port.notification.InvitationNotificationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "app.user-auth.invitation.mail",
    name = "provider",
    havingValue = "SMTP"
)
public class SmtpInvitationNotificationAdapter implements InvitationNotificationPort {

    private final JavaMailSender mailSender;
    private final InvitationNotificationProperties properties;

    @Override
    public void send(InvitationNotification notification) {
        if (!properties.getMail().isEnabled()) {
            return;
        }

        String acceptUrl = String.format(properties.getAcceptUrlTemplate(), notification.invitationId());
        mailSender.send(SmtpMailHelper.createMessage(
            mailSender,
            properties.getMail().getFrom(),
            notification.inviteeEmail(),
            "[Attestry] Invitation to join tenant",
            """
                You have been invited to join an Attestry tenant.

                Invitation ID: %s
                Tenant ID: %s
                Accept link: %s

                If you did not expect this email, you can ignore it.
                """.formatted(notification.invitationId(), notification.tenantId(), acceptUrl),
            notification.dedupeKey()
        ));
    }
}
