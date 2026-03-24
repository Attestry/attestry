package io.attestry.job.notification;

import io.attestry.userauth.application.port.notification.InvitationNotificationPort;
import io.attestry.userauth.application.port.notification.PassportManualNotificationPort;
import io.attestry.userauth.application.port.notification.SignUpEmailVerificationNotificationPort;
import io.attestry.userauth.domain.auth.model.SignUpEmailVerificationNotificationPayload;
import io.attestry.userauth.domain.membership.model.InvitationNotificationPayload;
import io.attestry.userauth.domain.membership.model.NotificationOutbox;
import io.attestry.userauth.domain.membership.model.PassportManualNotificationPayload;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class NotificationOutboxDispatcher {

    private final InvitationNotificationPort invitationNotificationPort;
    private final SignUpEmailVerificationNotificationPort signUpEmailVerificationNotificationPort;
    private final PassportManualNotificationPort passportManualNotificationPort;

    NotificationOutboxDispatcher(
        InvitationNotificationPort invitationNotificationPort,
        SignUpEmailVerificationNotificationPort signUpEmailVerificationNotificationPort,
        PassportManualNotificationPort passportManualNotificationPort
    ) {
        this.invitationNotificationPort = invitationNotificationPort;
        this.signUpEmailVerificationNotificationPort = signUpEmailVerificationNotificationPort;
        this.passportManualNotificationPort = passportManualNotificationPort;
    }

    void dispatch(NotificationOutbox entry) {
        switch (entry.notificationType()) {
            case INVITATION -> dispatchInvitation(entry);
            case SIGNUP_EMAIL_VERIFICATION -> dispatchSignupEmailVerification(entry);
            case PASSPORT_MANUAL_DELIVERY -> dispatchPassportManual(entry);
        }
    }

    private void dispatchInvitation(NotificationOutbox entry) {
        InvitationNotificationPayload payload = (InvitationNotificationPayload) entry.payload();
        invitationNotificationPort.send(
            new InvitationNotificationPort.InvitationNotification(
                payload.invitationId(),
                payload.tenantId(),
                payload.inviteeEmail(),
                payload.invitationId()
            )
        );
    }

    private void dispatchSignupEmailVerification(NotificationOutbox entry) {
        SignUpEmailVerificationNotificationPayload payload =
            (SignUpEmailVerificationNotificationPayload) entry.payload();
        signUpEmailVerificationNotificationPort.send(
            new SignUpEmailVerificationNotificationPort.SignUpEmailVerificationNotification(
                payload.verificationId(),
                payload.email(),
                payload.code(),
                payload.expiresInSeconds(),
                payload.verificationId()
            )
        );
    }

    private void dispatchPassportManual(NotificationOutbox entry) {
        PassportManualNotificationPayload payload = (PassportManualNotificationPayload) entry.payload();
        passportManualNotificationPort.send(
            new PassportManualNotificationPort.PassportManualNotification(
                payload.passportId(),
                payload.recipientEmail(),
                payload.serialNumber(),
                payload.modelName(),
                payload.message(),
                payload.evidenceGroupId(),
                payload.attachments().stream()
                    .map(attachment -> new PassportManualNotificationPort.AttachmentReference(
                        attachment.evidenceId(),
                        attachment.fileName(),
                        attachment.objectKey(),
                        attachment.contentType()
                    ))
                    .toList(),
                List.of(),
                payload.passportId() + ":" + payload.recipientEmail() + ":" + payload.evidenceGroupId()
            )
        );
    }
}
