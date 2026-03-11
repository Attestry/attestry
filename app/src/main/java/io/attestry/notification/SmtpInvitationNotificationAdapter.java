package io.attestry.notification;

import io.attestry.userauth.application.port.notification.InvitationNotificationPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    prefix = "app.user-auth.invitation.mail",
    name = "provider",
    havingValue = "SMTP"
)
public class SmtpInvitationNotificationAdapter implements InvitationNotificationPort {

    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String fromAddress;
    private final String acceptUrlTemplate;

    public SmtpInvitationNotificationAdapter(
        JavaMailSender mailSender,
        @Value("${app.user-auth.invitation.mail.enabled:true}") boolean enabled,
        @Value("${app.user-auth.invitation.mail.from:no-reply@attestry.local}") String fromAddress,
        @Value("${app.user-auth.invitation.accept-url-template:http://localhost:8080/invitations/%s/accept}") String acceptUrlTemplate
    ) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.fromAddress = fromAddress;
        this.acceptUrlTemplate = acceptUrlTemplate;
    }

    @Override
    public void send(InvitationNotification notification) {
        if (!enabled) {
            return;
        }

        String acceptUrl = String.format(acceptUrlTemplate, notification.invitationId());
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(notification.inviteeEmail());
        message.setSubject("[Attestry] Invitation to join tenant");
        message.setText("""
            You have been invited to join an Attestry tenant.

            Invitation ID: %s
            Tenant ID: %s
            Accept link: %s

            If you did not expect this email, you can ignore it.
            """.formatted(notification.invitationId(), notification.tenantId(), acceptUrl));

        mailSender.send(message);
    }
}

