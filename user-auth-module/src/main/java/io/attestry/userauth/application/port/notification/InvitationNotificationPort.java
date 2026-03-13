package io.attestry.userauth.application.port.notification;

public interface InvitationNotificationPort {

    void send(InvitationNotification notification);

    record InvitationNotification(
        String invitationId,
        String tenantId,
        String inviteeEmail
    ) {
    }
}
